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

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.cnr.iasi.saks.groucho.common.Context;
import it.cnr.iasi.saks.groucho.common.StateCarver;
import it.cnr.iasi.saks.groucho.config.PropertyUtil;
import it.cnr.iasi.saks.groucho.lsh.StateObserver;
import it.cnr.iasi.saks.groucho.lsh.exceptions.LSHException;
import it.cnr.iasi.saks.groucho.lsh.rest_client.StateObserverRestClientFactory;

/**
 * Test polimorfi sul contratto di {@link AbstractGovernanceManagerHistory}: ogni sottoclasse
 * concreta fornisce l'istanza da verificare tramite {@link #createGovernanceManager()} ed
 * eredita l'intera suite.
 *
 * Contratto verificato: {@code evaluateActivation} restituisce true solo se lo stato carvato
 * dal Context non e' ancora stato registrato sul server groucho-lsh-rest; una volta registrato,
 * lo stesso stato non deve piu' attivare una sessione in-vivo.
 *
 * Test di integrazione: richiedono un'istanza di groucho-lsh-rest dedicata ai test, in ascolto su
 * {@value #TEST_SERVER_URL_BASE_PATH}: se non e' gia' attiva, la suite la avvia in {@code @BeforeClass}
 * (script groucho-lab/src/test/resources/scripts/start-lsh-test-server.sh) e la spegne in {@code @AfterClass}. Lo storico
 * degli esperimenti, sul server della porta 8080, non viene mai toccato.
 *
 * I test NON resettano il server, per non perdere gli stati gia' registrati: ogni test
 * lavora su stati che contengono valori casuali generati al momento ({@link #setUp()}),
 * quindi mai visti prima dal server. Gli stati registrati dai test restano sul server.
 */
public abstract class AbstractGovernanceManagerHistoryTest_IT {

	/** Server groucho-lsh-rest dedicato ai test, separato da quello degli esperimenti (porta 8080). */
	public static final int TEST_SERVER_PORT = 8081;
	public static final String TEST_SERVER_URL_BASE_PATH = "http://localhost:" + TEST_SERVER_PORT + "/groucho-state-observer";

	/** Script (relativo alla cartella del modulo groucho-lab) che avvia il server di test. */
	private static final String TEST_SERVER_SCRIPT = "src/test/resources/scripts/start-lsh-test-server.sh";
	private static final String TEST_SERVER_LOG = "target/lsh-test-server.log";
	private static final long TEST_SERVER_STARTUP_TIMEOUT_MS = 120000;
	private static final long TEST_SERVER_SHUTDOWN_TIMEOUT_MS = 15000;
	private static final long TEST_SERVER_POLL_MS = 500;

	/** Server avviato da questa suite, null se gia' attivo prima dei test (e quindi da non spegnere). */
	private static Process testServerProcess = null;

	private static final String INSTRUMENTED_METHOD = "instrumentedMethod";

	private static final int MULTIPLE_CHECKS = 5;

	/** Numero di UUID concatenati nel nonce: la parte casuale deve dominare lo stato carvato. */
	private static final int NONCE_UUIDS = 4;

	protected AbstractGovernanceManagerHistory gm;

	/** Rende unico lo stato "di default" del singolo test. */
	private String defaultNonce;
	/** Rende unico lo stato "diverso" del singolo test, indipendente dal precedente. */
	private String differentNonce;

	/**
	 * @return una nuova istanza del Governance Manager concreto da sottoporre a test
	 */
	protected abstract AbstractGovernanceManagerHistory createGovernanceManager();

	/**
	 * Punta i manager al server dedicato ai test, mai a quello degli esperimenti (porta 8080),
	 * e lo avvia con {@value #TEST_SERVER_SCRIPT} se non e' gia' in ascolto. Un server gia' attivo
	 * viene usato cosi' com'e' e non viene spento a fine suite.
	 * Si puo' indicare un altro indirizzo con -Dgroucho.lsh.rest_client.urlBasePath=...: in quel
	 * caso il server va avviato a mano.
	 */
	@BeforeClass
	public static void setUpTestServer() throws IOException, InterruptedException {
		String urlBasePath = System.getProperty(PropertyUtil.LSH_REST_CLIENT_URL_BASE_PATH_LABEL);
		if ((urlBasePath == null) || urlBasePath.trim().isEmpty()) {
			urlBasePath = TEST_SERVER_URL_BASE_PATH;
			System.setProperty(PropertyUtil.LSH_REST_CLIENT_URL_BASE_PATH_LABEL, urlBasePath);
		}

		if (isServerReachable(urlBasePath)) {
			System.out.println("[" + AbstractGovernanceManagerHistoryTest_IT.class.getSimpleName()
					+ "] server di test gia' attivo su " + urlBasePath + ": non verra' spento a fine suite");
			return;
		}
		if (!TEST_SERVER_URL_BASE_PATH.equals(urlBasePath)) {
			Assert.fail("Server groucho-lsh-rest di test non raggiungibile su " + urlBasePath
					+ ": per un indirizzo personalizzato il server va avviato a mano");
		}
		startTestServer();
	}

	@AfterClass
	public static void tearDownTestServer() throws InterruptedException {
		stopTestServer();
	}

	// ------------------------------------------------------- server di test

	private static void startTestServer() throws IOException, InterruptedException {
		// surefire e gli IDE eseguono i test dalla cartella del modulo groucho-lab
		File moduleDir = new File(System.getProperty("user.dir"));
		File script = new File(moduleDir, TEST_SERVER_SCRIPT);
		if (!script.isFile()) {
			Assert.fail("Script di avvio del server di test non trovato: " + script.getAbsolutePath());
		}
		File log = new File(moduleDir, TEST_SERVER_LOG);
		log.getParentFile().mkdirs();

		System.out.println("[" + AbstractGovernanceManagerHistoryTest_IT.class.getSimpleName()
				+ "] avvio del server di test (" + TEST_SERVER_URL_BASE_PATH + "), log: " + log.getAbsolutePath());
		ProcessBuilder builder = new ProcessBuilder("bash", script.getAbsolutePath(), String.valueOf(TEST_SERVER_PORT));
		builder.directory(moduleDir);
		builder.redirectErrorStream(true);
		builder.redirectOutput(ProcessBuilder.Redirect.to(log));
		testServerProcess = builder.start();

		long deadline = System.currentTimeMillis() + TEST_SERVER_STARTUP_TIMEOUT_MS;
		while (System.currentTimeMillis() < deadline) {
			if (!testServerProcess.isAlive()) {
				int exitCode = testServerProcess.exitValue();
				testServerProcess = null;
				Assert.fail("Il server di test si e' chiuso durante l'avvio (exit " + exitCode + "), vedere " + log.getAbsolutePath());
			}
			if (isServerReachable(TEST_SERVER_URL_BASE_PATH)) {
				return;
			}
			Thread.sleep(TEST_SERVER_POLL_MS);
		}
		stopTestServer();
		Assert.fail("Il server di test non ha risposto entro " + TEST_SERVER_STARTUP_TIMEOUT_MS + " ms, vedere "
				+ log.getAbsolutePath());
	}

	/** Spegne il server solo se e' stato avviato da questa suite. */
	private static void stopTestServer() throws InterruptedException {
		if (testServerProcess == null) {
			return;
		}
		// lo script fa exec di java: il processo avviato e' la JVM del server
		testServerProcess.destroy();
		if (!testServerProcess.waitFor(TEST_SERVER_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
			testServerProcess.destroyForcibly().waitFor();
		}
		testServerProcess = null;
	}

	/** Controllo in sola lettura: senza server ogni evaluateActivation darebbe false. */
	private static boolean isServerReachable(String urlBasePath) {
		StateObserver probe = new StateObserverRestClientFactory(urlBasePath).getStateObserver();
		try {
			probe.isStateUnknown("probe_" + randomNonce());
			return true;
		} catch (LSHException e) {
			return false;
		}
	}

	@Before
	public void setUp() {
		// nessun reset del server: l'isolamento e' dato da stati nuovi a ogni test
		this.defaultNonce = randomNonce();
		this.differentNonce = randomNonce();
		GMHistoryDummyInvivoTest.resetInvocations();
		this.gm = this.createGovernanceManager();
	}

	// ------------------------------------------------------------------ utilita'

	/**
	 * Stringa casuale lunga: lo stato carvato di un test non deve risultare simile (per LSH)
	 * a quelli di esecuzioni precedenti, che differirebbero solo per questa parte.
	 */
	private static String randomNonce() {
		StringBuilder nonce = new StringBuilder();
		for (int i = 0; i < NONCE_UUIDS; i++) {
			nonce.append(UUID.randomUUID().toString().replace("-", ""));
		}
		return nonce.toString();
	}

	protected Context createContext(Object instrumentedObject) {
		return new Context(instrumentedObject, instrumentedObject.getClass().getName(), INSTRUMENTED_METHOD,
				GMHistoryDummyInvivoTest.class.getName(), GMHistoryDummyInvivoTest.INVIVO_TEST_METHOD);
	}

	/** Due chiamate nello stesso test restituiscono oggetti distinti con lo stesso stato. */
	protected GMHistoryTestClass createDefaultObject() {
		return new GMHistoryTestClass(1, "defaultLabel_" + this.defaultNonce, true);
	}

	/** Stato volutamente molto diverso da quello di default, per non cadere nello stesso bucket LSH. */
	protected GMHistoryTestClass createDifferentObject() {
		return new GMHistoryTestClass(-987654321, "aCompletelyDifferentLabel_" + this.differentNonce, false);
	}

	/** Registra sul server lo stato del Context, passando dalla stessa rappresentazione usata dal manager. */
	protected void markContextState(AbstractGovernanceManagerHistory manager, Context context)
			throws ClassNotFoundException, LSHException {
		String carvedState = new StateCarver(context).carveAllFields();
		manager.markState(manager.processState(carvedState));
	}

	// -------------------------------------------------------------------- test

	@Test
	public void nullContextIsNotActivatedTest() {
		boolean condition = this.gm.evaluateActivation(null);
		Assert.assertFalse(condition);
	}

	@Test
	public void unknownStateIsActivatedTest() {
		Context context = this.createContext(this.createDefaultObject());

		boolean condition = this.gm.evaluateActivation(context);

		Assert.assertTrue("Uno stato mai registrato deve attivare la sessione", condition);
	}

	@Test
	public void markedStateIsNotActivatedTest() throws ClassNotFoundException, LSHException {
		Context context = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, context);

		boolean condition = this.gm.evaluateActivation(context);

		Assert.assertFalse("Uno stato gia' registrato non deve attivare la sessione", condition);
	}

	@Test
	public void markedStateIsNotActivatedMultipleChecksTest() throws ClassNotFoundException, LSHException {
		Context context = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, context);

		for (int i = 0; i < MULTIPLE_CHECKS; i++) {
			boolean condition = this.gm.evaluateActivation(context);
			Assert.assertFalse("Controllo n." + i + ": lo stato registrato non deve attivare la sessione", condition);
		}
	}

	@Test
	public void stateMarkedByInvivoSessionIsNotActivatedTest() {
		Context context = this.createContext(this.createDefaultObject());

		boolean firstEvaluation = this.gm.evaluateActivation(context);
		Assert.assertTrue("Prima valutazione: lo stato e' nuovo", firstEvaluation);

		// la sessione in-vivo esegue il test e, al termine, registra lo stato appena valutato
		this.gm.doInvivoTestingSession(context);
		Assert.assertEquals("Il test in-vivo deve essere stato eseguito una volta", 1,
				GMHistoryDummyInvivoTest.getInvocations());

		boolean secondEvaluation = this.gm.evaluateActivation(context);
		Assert.assertFalse("Dopo la sessione lo stato e' noto e non deve riattivarla", secondEvaluation);
	}

	@Test
	public void evaluationDoesNotMarkStateTest() {
		Context context = this.createContext(this.createDefaultObject());

		for (int i = 0; i < MULTIPLE_CHECKS; i++) {
			boolean condition = this.gm.evaluateActivation(context);
			Assert.assertTrue("Controllo n." + i + ": la sola valutazione non deve registrare lo stato", condition);
		}
	}

	@Test
	public void equivalentObjectIsNotActivatedTest() throws ClassNotFoundException, LSHException {
		Context markedContext = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, markedContext);

		// istanza diversa ma con gli stessi valori: lo storico e' basato sullo stato, non sull'identita'
		Context equivalentContext = this.createContext(this.createDefaultObject());
		boolean condition = this.gm.evaluateActivation(equivalentContext);

		Assert.assertFalse("Un oggetto con lo stesso stato non deve attivare la sessione", condition);
	}

	@Test
	public void differentStateIsActivatedTest() throws ClassNotFoundException, LSHException {
		Context markedContext = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, markedContext);

		Context differentContext = this.createContext(this.createDifferentObject());
		boolean differentCondition = this.gm.evaluateActivation(differentContext);
		Assert.assertTrue("Uno stato diverso da quelli registrati deve attivare la sessione", differentCondition);

		boolean markedCondition = this.gm.evaluateActivation(markedContext);
		Assert.assertFalse("Lo stato registrato resta noto", markedCondition);
	}

	@Test
	public void objectModifiedAfterMarkingIsActivatedTest() throws ClassNotFoundException, LSHException {
		GMHistoryTestClass instrumentedObject = this.createDefaultObject();
		Context context = this.createContext(instrumentedObject);
		this.markContextState(this.gm, context);
		Assert.assertFalse(this.gm.evaluateActivation(context));

		GMHistoryTestClass different = this.createDifferentObject();
		instrumentedObject.setContextToAdd(different.getContextToAdd());
		instrumentedObject.setLabel(different.getLabel());
		instrumentedObject.setFlag(different.isFlag());

		// stesso Context e stesso oggetto: lo stato viene carvato a ogni valutazione
		boolean condition = this.gm.evaluateActivation(context);
		Assert.assertTrue("Dopo la modifica l'oggetto e' in uno stato nuovo", condition);
	}

	@Test
	public void historyIsSharedAmongManagerInstancesTest() throws ClassNotFoundException, LSHException {
		Context context = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, context);

		// lo storico vive sul server, non nell'istanza del manager
		AbstractGovernanceManagerHistory anotherGm = this.createGovernanceManager();
		boolean condition = anotherGm.evaluateActivation(context);

		Assert.assertFalse("Un altro manager dello stesso tipo deve vedere lo stato gia' registrato", condition);
	}

	@Test
	public void processStateIsDeterministicTest() throws ClassNotFoundException {
		Context context = this.createContext(this.createDefaultObject());
		String carvedState = new StateCarver(context).carveAllFields();

		String first = this.gm.processState(carvedState);
		String second = this.createGovernanceManager().processState(carvedState);

		Assert.assertNotNull(first);
		Assert.assertEquals(first, second);
	}
}
