/*
 * This file is part of the GROUCHO project.
 *
 * GROUCHO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GROUCHO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GROUCHO.  If not, see <https://www.gnu.org/licenses/>
 *
 */
package it.cnr.iasi.saks.groucho.gm_history;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import it.cnr.iasi.saks.groucho.common.Context;
import it.cnr.iasi.saks.groucho.config.PropertyUtil;
import it.cnr.iasi.saks.groucho.lsh.exceptions.LSHException;

/**
 * Governance Manager con coda di richieste di dimensione k e due possibili
 * politiche di attivazione:
 * <ul>
 * <li>se riceve una probabilita' p, attiva la sessione in-vivo con probabilita' p;</li>
 * <li>altrimenti usa lo storico LSH ereditato da {@link GovernanceManagerBasic}
 * (attiva solo per stati non ancora visti).</li>
 * </ul>
 *
 * Coda: nel Governance Manager di base una richiesta che arriva mentre e' in corso
 * un'altra sessione viene scartata. Qui viene invece messa in una coda limitata a k
 * elementi (se la coda e' piena viene scartata). Un solo thread alla volta serve la coda,
 * eseguendo una sessione per ogni richiesta in ordine di arrivo. La politica di attivazione
 * e' valutata al momento dell'esecuzione.
 *
 * Attesa (predefinita): il thread che accoda resta fermo finche' la sua richiesta non e'
 * stata servita; se nel frattempo nessuno serve la coda, la serve lui. Ogni thread ha quindi
 * al massimo una richiesta in coda, e non va avanti prima che la sessione sia finita: lo stato
 * testato e' quello del momento della richiesta, salvo modifiche fatte da altri thread.
 * Senza attesa il thread accoda e prosegue: la coda contiene i Context, che riferiscono gli
 * oggetti vivi, e la sessione vede lo stato che l'oggetto ha quando la richiesta viene servita.
 *
 * Il GovernanceManagerFactory crea l'istanza con il costruttore senza argomenti, che
 * legge i parametri prima dalle proprieta' di sistema (-D sulla riga di comando) e poi
 * dal file di configurazione di groucho:
 * <ul>
 * <li>{@value #ACTIVATION_PROBABILITY_LABEL}: probabilita' in [0,1]; se assente si usa LSH;</li>
 * <li>{@value #ACTIVATION_SEED_LABEL}: seed delle estrazioni casuali, per renderle ripetibili;</li>
 * <li>{@value #QUEUE_SIZE_LABEL}: k &gt;= 1; se assente vale {@value #DEFAULT_QUEUE_SIZE};</li>
 * <li>{@value #QUEUE_WAIT_LABEL}: true (predefinito) o false, se chi accoda attende la propria sessione.</li>
 * </ul>
 *
 * Log: a ogni richiesta accodata e a ogni richiesta presa dalla coda viene stampata la riga
 * "in coda: n/k", con il numero di richieste in coda in quel momento.
 */
public class GovernanceManagerQueuedByHistory extends GovernanceManagerBasic {

	public static final String ACTIVATION_PROBABILITY_LABEL = "groucho.lab.governance.activation.probability";
	public static final String ACTIVATION_SEED_LABEL = "groucho.lab.governance.activation.seed";
	public static final String QUEUE_SIZE_LABEL = "groucho.lab.governance.queue.size";
	public static final String QUEUE_WAIT_LABEL = "groucho.lab.governance.queue.wait";
	public static final int DEFAULT_QUEUE_SIZE = 10;

	/** Richiesta in coda: il Context e il segnale di richiesta servita. */
	private static final class Request {
		private final Context context;
		private boolean served = false; // protetto da drainerLock

		private Request(Context context) {
			this.context = context;
		}
	}

	/** null: attivazione tramite storico LSH. */
	private final Float activationProbability;
	private final Random random;

	private final int queueSize;
	private final boolean waitForService;
	private final BlockingQueue<Request> queue;

	/** Thread che sta servendo la coda, null se nessuno. Monitor anche per le attese. */
	private Thread drainer = null;
	private final Object drainerLock = new Object();

	private final AtomicLong enqueuedRequests = new AtomicLong();
	private final AtomicLong droppedRequests = new AtomicLong();
	private final AtomicLong servedRequests = new AtomicLong();

	/** Istante in cui e' stata decisa l'attivazione della sessione in corso, -1 se nessuna. Solo il thread servente lo usa. */
	private long sessionActivatedAt = -1;

	/** Costruttore usato dal GovernanceManagerFactory: parametri dalla configurazione. */
	public GovernanceManagerQueuedByHistory() {
		this(readProbability(), readQueueSize(), readSeed(), readWait());
	}

	/** Attivazione tramite storico LSH, coda di dimensione k. */
	public GovernanceManagerQueuedByHistory(int k) {
		this(null, k);
	}

	/**
	 * @param probability probabilita' di attivazione in [0,1], oppure null per usare lo storico LSH
	 * @param k dimensione della coda (&gt;= 1)
	 */
	public GovernanceManagerQueuedByHistory(Float probability, int k) {
		this(probability, k, null);
	}

	/**
	 * @param probability probabilita' di attivazione in [0,1], oppure null per usare lo storico LSH
	 * @param k dimensione della coda (&gt;= 1)
	 * @param seed seed delle estrazioni casuali, oppure null per un seed qualsiasi
	 */
	public GovernanceManagerQueuedByHistory(Float probability, int k, Long seed) {
		this(probability, k, seed, true);
	}

	/**
	 * @param probability probabilita' di attivazione in [0,1], oppure null per usare lo storico LSH
	 * @param k dimensione della coda (&gt;= 1)
	 * @param seed seed delle estrazioni casuali, oppure null per un seed qualsiasi
	 * @param waitForService true se chi accoda resta fermo finche' la sua richiesta non e' servita
	 */
	public GovernanceManagerQueuedByHistory(Float probability, int k, Long seed, boolean waitForService) {
		super();
		if ((probability != null) && ((probability < 0f) || (probability > 1f))) {
			throw new IllegalArgumentException("probabilita' fuori da [0,1]: " + probability);
		}
		if (k < 1) {
			throw new IllegalArgumentException("dimensione della coda non valida: " + k);
		}
		this.activationProbability = probability;
		this.random = (seed == null) ? new Random() : new Random(seed);
		this.queueSize = k;
		this.waitForService = waitForService;
		this.queue = new ArrayBlockingQueue<Request>(k);
		System.out.println(this.tag() + "attivazione: "
				+ (this.isProbabilityMode() ? "probabilita' " + probability + (seed == null ? "" : " seed=" + seed) : "storico LSH")
				+ ", coda k=" + k + (waitForService ? " con attesa" : " senza attesa"));
	}

	// ------------------------------------------------------------------ coda

	@Override
	public void runInvivoTestingSession(Context context) {
		synchronized (this.drainerLock) {
			if (this.drainer == Thread.currentThread()) {
				// Richiesta generata dal test in-vivo (o dalla valutazione) in esecuzione su questo
				// thread: va ignorata. Non si passa alla classe base: il suo lock e' rientrante,
				// e la chiamata annidata, uscendo, lo rilascerebbe e azzererebbe la sessione in corso.
				return;
			}
		}

		Request request = new Request(context);
		if (this.queue.offer(request)) {
			this.enqueuedRequests.incrementAndGet();
			this.logQueueSize();
		} else {
			this.droppedRequests.incrementAndGet();
			System.out.println(this.tag() + "coda piena (k=" + this.queueSize + "), richiesta scartata");
			request = null;
		}

		if (this.waitForService && request != null) {
			this.awaitService(request);
		} else {
			this.drainQueue();
		}
	}

	/**
	 * Attende che la richiesta sia servita. Se nessuno sta servendo la coda, la serve il
	 * thread corrente: la coda e' FIFO, quindi svuotandola serve anche la propria richiesta.
	 */
	private void awaitService(Request request) {
		while (true) {
			synchronized (this.drainerLock) {
				while (!request.served && this.drainer != null) {
					try {
						this.drainerLock.wait();
					} catch (InterruptedException e) {
						// Es. il thread di un fantasma fermato a fine partita: smette di attendere.
						// La richiesta resta in coda e verra' servita comunque.
						Thread.currentThread().interrupt();
						return;
					}
				}
				if (request.served) {
					return;
				}
				this.drainer = Thread.currentThread();
			}
			this.serveQueue();
		}
	}

	/**
	 * Senza attesa: se nessun thread sta servendo la coda, la serve il thread corrente.
	 * Il ciclo esterno evita che una richiesta accodata subito dopo l'ultimo poll()
	 * resti in coda senza nessuno che la serva.
	 */
	private void drainQueue() {
		while (!this.queue.isEmpty()) {
			synchronized (this.drainerLock) {
				if (this.drainer != null) {
					return; // un altro thread sta gia' servendo la coda
				}
				this.drainer = Thread.currentThread();
			}
			this.serveQueue();
		}
	}

	/** Da chiamare dopo essere diventati il thread che serve la coda: la svuota e rinuncia al ruolo. */
	private void serveQueue() {
		try {
			Request next;
			while ((next = this.queue.poll()) != null) {
				this.servedRequests.incrementAndGet();
				this.logQueueSize();
				try {
					this.executeSession(next.context);
				} finally {
					synchronized (this.drainerLock) {
						next.served = true;
						this.drainerLock.notifyAll();
					}
				}
			}
		} finally {
			synchronized (this.drainerLock) {
				this.drainer = null;
				this.drainerLock.notifyAll();
			}
		}
	}

	/**
	 * Esegue la sessione in-vivo per una richiesta presa dalla coda. Le sottoclassi
	 * possono ridefinirlo per preparare il Context o prendere lock dell'applicazione,
	 * chiamando poi super.executeSession.
	 */
	protected void executeSession(Context context) {
		this.sessionActivatedAt = -1;
		try {
			super.runInvivoTestingSession(context);
		} finally {
			if (this.sessionActivatedAt >= 0) {
				// Dalla decisione di attivare alla fine del rollback: checkpoint, test e rollback
				System.out.println(this.tag() + "sessione in-vivo: durataMs="
						+ String.format("%.3f", (System.nanoTime() - this.sessionActivatedAt) / 1e6));
				this.sessionActivatedAt = -1;
			}
		}
	}

	/** Come la classe padre; in piu' annota l'istante dell'attivazione per misurare la durata della sessione. */
	@Override
	public boolean evaluateActivation(Context context) {
		boolean activated = super.evaluateActivation(context);
		if (activated) {
			this.sessionActivatedAt = System.nanoTime();
		}
		return activated;
	}

	// ------------------------------------------------------------- log coda

	/** Stampa quante richieste sono in coda: chiamato a ogni richiesta accodata e a ogni richiesta presa. */
	private void logQueueSize() {
		System.out.println(this.tag() + "in coda: " + this.queue.size() + "/" + this.queueSize);
	}

	// ----------------------------------------------------- attivazione

	/**
	 * In modalita' probabilistica lo stato viene comunque catturato (per stamparlo e per
	 * scartare le chiamate che le sottoclassi non vogliono valutare), ma il server non
	 * viene contattato.
	 */
	@Override
	protected boolean decideActivation(String carvedState, String processedState) throws LSHException {
		if (!this.isProbabilityMode()) {
			return super.decideActivation(carvedState, processedState);
		}
		// nextDouble, come il vecchio manager di lab-jpacman: stesso seed, stesse estrazioni
		boolean activated = this.random.nextDouble() < this.activationProbability.doubleValue();
		System.out.println(this.tag() + "stato: " + carvedState + " -> attivata: " + activated
				+ " (p=" + this.activationProbability + ")");
		return activated;
	}

	@Override
	protected void markState(String processedState) throws LSHException {
		// In modalita' probabilistica lo stato non viene calcolato ne' registrato sul server.
		if (!this.isProbabilityMode()) {
			super.markState(processedState);
		}
	}

	// --------------------------------------------------------- utilita'

	public boolean isProbabilityMode() {
		return this.activationProbability != null;
	}

	public Float getActivationProbability() {
		return this.activationProbability;
	}

	public boolean isWaitingForService() {
		return this.waitForService;
	}

	public int getQueueSize() {
		return this.queueSize;
	}

	public int getPendingRequests() {
		return this.queue.size();
	}

	public long getEnqueuedRequests() {
		return this.enqueuedRequests.get();
	}

	public long getDroppedRequests() {
		return this.droppedRequests.get();
	}

	public long getServedRequests() {
		return this.servedRequests.get();
	}

	/** @return una riga con i contatori della coda, per i riepiloghi degli esperimenti. */
	public String queueSummary() {
		return "k=" + this.queueSize + " attesa=" + this.waitForService + " accodate=" + this.getEnqueuedRequests() + " scartate="
				+ this.getDroppedRequests() + " eseguite=" + this.getServedRequests() + " inAttesa="
				+ this.getPendingRequests();
	}

	/** @return il valore dalla riga di comando (-D), altrimenti dal file di configurazione, altrimenti null. */
	private static String readParameter(String label) {
		String value = System.getProperty(label);
		if ((value == null) || value.trim().isEmpty()) {
			value = PropertyUtil.getInstance().getProperty(label);
		}
		return ((value == null) || value.trim().isEmpty()) ? null : value.trim();
	}

	private static Float readProbability() {
		String value = readParameter(ACTIVATION_PROBABILITY_LABEL);
		return (value == null) ? null : Float.valueOf(value);
	}

	private static Long readSeed() {
		String value = readParameter(ACTIVATION_SEED_LABEL);
		return (value == null) ? null : Long.valueOf(value);
	}

	private static boolean readWait() {
		String value = readParameter(QUEUE_WAIT_LABEL);
		return (value == null) || Boolean.parseBoolean(value);
	}

	private static int readQueueSize() {
		String value = readParameter(QUEUE_SIZE_LABEL);
		return (value == null) ? DEFAULT_QUEUE_SIZE : Integer.parseInt(value);
	}
}
