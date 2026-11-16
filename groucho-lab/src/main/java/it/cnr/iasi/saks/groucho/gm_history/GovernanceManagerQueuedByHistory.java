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
 * Governance Manager with a request queue of size k and two possible
 * activation policies:
 * <ul>
 * <li>when given a probability p, it activates the in-vivo session with probability p;</li>
 * <li>otherwise it uses the LSH history inherited from {@link GovernanceManagerBasic}
 * (it activates only for states not seen yet).</li>
 * </ul>
 *
 * Queue: in the base Governance Manager a request arriving while another session is
 * running is dropped. Here it is put in a queue bounded to k elements instead (if the
 * queue is full it is dropped). Only one thread at a time serves the queue, running one
 * session per request in arrival order. The activation policy is evaluated when the
 * session is run.
 *
 * Waiting (default): the thread that enqueues stays blocked until its request has been
 * served; if meanwhile nobody is serving the queue, it serves the queue itself. Each thread
 * therefore has at most one request in the queue, and does not go on before the session is
 * over: the tested state is the one at the time of the request, unless other threads change it.
 * Without waiting the thread enqueues and goes on: the queue holds the Contexts, which refer
 * to the live objects, so the session sees the state the object has when the request is served.
 *
 * The GovernanceManagerFactory creates the instance with the no-argument constructor, which
 * reads the parameters first from the system properties (-D on the command line) and then
 * from the groucho configuration file:
 * <ul>
 * <li>{@value #ACTIVATION_PROBABILITY_LABEL}: probability in [0,1]; when missing LSH is used;</li>
 * <li>{@value #ACTIVATION_SEED_LABEL}: seed of the random draws, to make them repeatable;</li>
 * <li>{@value #QUEUE_SIZE_LABEL}: k &gt;= 1; when missing it is {@value #DEFAULT_QUEUE_SIZE};</li>
 * <li>{@value #QUEUE_WAIT_LABEL}: true (default) or false, whether the enqueuing thread waits for its session.</li>
 * </ul>
 *
 * Log: for every enqueued request and every request taken from the queue the line
 * "queued: n/k" is printed, with the number of requests in the queue at that moment.
 */
public class GovernanceManagerQueuedByHistory extends GovernanceManagerBasic {

	public static final String ACTIVATION_PROBABILITY_LABEL = "groucho.lab.governance.activation.probability";
	public static final String ACTIVATION_SEED_LABEL = "groucho.lab.governance.activation.seed";
	public static final String QUEUE_SIZE_LABEL = "groucho.lab.governance.queue.size";
	public static final String QUEUE_WAIT_LABEL = "groucho.lab.governance.queue.wait";
	public static final int DEFAULT_QUEUE_SIZE = 10;

	/** Queued request: the Context and the "request served" signal. */
	private static final class Request {
		private final Context context;
		private boolean served = false; // guarded by drainerLock

		private Request(Context context) {
			this.context = context;
		}
	}

	/** null: activation through the LSH history. */
	private final Float activationProbability;
	private final Random random;

	private final int queueSize;
	private final boolean waitForService;
	private final BlockingQueue<Request> queue;

	/** Thread serving the queue, null if none. Also used as the monitor for the waits. */
	private Thread drainer = null;
	private final Object drainerLock = new Object();

	private final AtomicLong enqueuedRequests = new AtomicLong();
	private final AtomicLong droppedRequests = new AtomicLong();
	private final AtomicLong servedRequests = new AtomicLong();

	/** Instant when the activation of the running session was decided, -1 if none. Only the serving thread uses it. */
	private long sessionActivatedAt = -1;

	/** Constructor used by the GovernanceManagerFactory: parameters from the configuration. */
	public GovernanceManagerQueuedByHistory() {
		this(readProbability(), readQueueSize(), readSeed(), readWait());
	}

	/** Activation through the LSH history, queue of size k. */
	public GovernanceManagerQueuedByHistory(int k) {
		this(null, k);
	}

	/**
	 * @param probability activation probability in [0,1], or null to use the LSH history
	 * @param k size of the queue (&gt;= 1)
	 */
	public GovernanceManagerQueuedByHistory(Float probability, int k) {
		this(probability, k, null);
	}

	/**
	 * @param probability activation probability in [0,1], or null to use the LSH history
	 * @param k size of the queue (&gt;= 1)
	 * @param seed seed of the random draws, or null for an arbitrary seed
	 */
	public GovernanceManagerQueuedByHistory(Float probability, int k, Long seed) {
		this(probability, k, seed, true);
	}

	/**
	 * @param probability activation probability in [0,1], or null to use the LSH history
	 * @param k size of the queue (&gt;= 1)
	 * @param seed seed of the random draws, or null for an arbitrary seed
	 * @param waitForService true if the enqueuing thread stays blocked until its request is served
	 */
	public GovernanceManagerQueuedByHistory(Float probability, int k, Long seed, boolean waitForService) {
		super();
		if ((probability != null) && ((probability < 0f) || (probability > 1f))) {
			throw new IllegalArgumentException("probability outside [0,1]: " + probability);
		}
		if (k < 1) {
			throw new IllegalArgumentException("invalid queue size: " + k);
		}
		this.activationProbability = probability;
		this.random = (seed == null) ? new Random() : new Random(seed);
		this.queueSize = k;
		this.waitForService = waitForService;
		this.queue = new ArrayBlockingQueue<Request>(k);
		System.out.println(this.tag() + "activation: "
				+ (this.isProbabilityMode() ? "probability " + probability + (seed == null ? "" : " seed=" + seed) : "LSH history")
				+ ", queue k=" + k + (waitForService ? " with waiting" : " without waiting"));
	}

	// ----------------------------------------------------------------- queue

	@Override
	public void runInvivoTestingSession(Context context) {
		synchronized (this.drainerLock) {
			if (this.drainer == Thread.currentThread()) {
				// Request generated by the in-vivo test (or by the evaluation) running on this
				// thread: it must be ignored. It is not passed to the base class: its lock is reentrant,
				// and the nested call, on exit, would release it and reset the running session.
				return;
			}
		}

		Request request = new Request(context);
		if (this.queue.offer(request)) {
			this.enqueuedRequests.incrementAndGet();
			this.logQueueSize();
		} else {
			this.droppedRequests.incrementAndGet();
			System.out.println(this.tag() + "queue full (k=" + this.queueSize + "), request dropped");
			request = null;
		}

		if (this.waitForService && request != null) {
			this.awaitService(request);
		} else {
			this.drainQueue();
		}
	}

	/**
	 * Waits until the request has been served. If nobody is serving the queue, the current
	 * thread serves it: the queue is FIFO, so by draining it the thread also serves its own request.
	 */
	private void awaitService(Request request) {
		while (true) {
			synchronized (this.drainerLock) {
				while (!request.served && this.drainer != null) {
					try {
						this.drainerLock.wait();
					} catch (InterruptedException e) {
						// E.g. the thread of a ghost stopped at the end of the game: it stops waiting.
						// The request stays in the queue and will be served anyway.
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
	 * Without waiting: if no thread is serving the queue, the current thread serves it.
	 * The outer loop prevents a request enqueued right after the last poll() from
	 * staying in the queue with nobody serving it.
	 */
	private void drainQueue() {
		while (!this.queue.isEmpty()) {
			synchronized (this.drainerLock) {
				if (this.drainer != null) {
					return; // another thread is already serving the queue
				}
				this.drainer = Thread.currentThread();
			}
			this.serveQueue();
		}
	}

	/** To be called after becoming the thread that serves the queue: drains it and gives up the role. */
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
	 * Runs the in-vivo session for a request taken from the queue. Subclasses may
	 * override it to prepare the Context or take application locks, then calling
	 * super.executeSession.
	 */
	protected void executeSession(Context context) {
		this.sessionActivatedAt = -1;
		try {
			super.runInvivoTestingSession(context);
		} finally {
			if (this.sessionActivatedAt >= 0) {
				// From the activation decision to the end of the rollback: checkpoint, test and rollback
				System.out.println(this.tag() + "in-vivo session: durationMs="
						+ String.format("%.3f", (System.nanoTime() - this.sessionActivatedAt) / 1e6));
				this.sessionActivatedAt = -1;
			}
		}
	}

	/** Like the parent class; it also records the activation instant to measure the duration of the session. */
	@Override
	public boolean evaluateActivation(Context context) {
		boolean activated = super.evaluateActivation(context);
		if (activated) {
			this.sessionActivatedAt = System.nanoTime();
		}
		return activated;
	}

	// ------------------------------------------------------------- queue log

	/** Prints how many requests are queued: called for every enqueued request and every request taken. */
	private void logQueueSize() {
		System.out.println(this.tag() + "queued: " + this.queue.size() + "/" + this.queueSize);
	}

	// ------------------------------------------------------- activation

	/**
	 * In probability mode the state is captured anyway (to print it and to discard the
	 * invocations that subclasses do not want to evaluate), but the server is not contacted.
	 */
	@Override
	protected boolean decideActivation(String carvedState, String processedState) throws LSHException {
		if (!this.isProbabilityMode()) {
			return super.decideActivation(carvedState, processedState);
		}
		// nextDouble, like the old lab-jpacman manager: same seed, same draws
		boolean activated = this.random.nextDouble() < this.activationProbability.doubleValue();
		System.out.println(this.tag() + "state: " + carvedState + " -> activated: " + activated
				+ " (p=" + this.activationProbability + ")");
		return activated;
	}

	@Override
	protected void markState(String processedState) throws LSHException {
		// In probability mode the state is neither computed nor marked on the server.
		if (!this.isProbabilityMode()) {
			super.markState(processedState);
		}
	}

	// ---------------------------------------------------------- utilities

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

	/** @return a line with the queue counters, for the experiment summaries. */
	public String queueSummary() {
		return "k=" + this.queueSize + " wait=" + this.waitForService + " enqueued=" + this.getEnqueuedRequests() + " dropped="
				+ this.getDroppedRequests() + " served=" + this.getServedRequests() + " pending="
				+ this.getPendingRequests();
	}

	/** @return the value from the command line (-D), otherwise from the configuration file, otherwise null. */
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
