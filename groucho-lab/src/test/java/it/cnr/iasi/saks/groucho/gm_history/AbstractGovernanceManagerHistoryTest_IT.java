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
 * Polymorphic tests on the contract of {@link AbstractGovernanceManagerHistory}: each concrete
 * subclass provides the instance under test through {@link #createGovernanceManager()} and
 * inherits the whole suite.
 *
 * Verified contract: {@code evaluateActivation} returns true only if the state carved from the
 * Context has not been marked on the groucho-lsh-rest server yet; once marked, the same state
 * must not activate an in-vivo session any more.
 *
 * Integration tests: they need a groucho-lsh-rest instance dedicated to the tests, listening on
 * {@value #TEST_SERVER_URL_BASE_PATH}: if it is not running yet, the suite starts it in
 * {@code @BeforeClass} (script groucho-lab/src/test/resources/scripts/start-lsh-test-server.sh)
 * and stops it in {@code @AfterClass}. The experiment history, on the server on port 8080, is
 * never touched.
 *
 * The tests do NOT reset the server, so that the states already marked are not lost: each test
 * works on states containing random values generated on the fly ({@link #setUp()}), hence never
 * seen before by the server. The states marked by the tests stay on the server.
 */
public abstract class AbstractGovernanceManagerHistoryTest_IT {

	/** groucho-lsh-rest server dedicated to the tests, separate from the experiment one (port 8080). */
	public static final int TEST_SERVER_PORT = 8081;
	public static final String TEST_SERVER_URL_BASE_PATH = "http://localhost:" + TEST_SERVER_PORT + "/groucho-state-observer";

	/** Script (relative to the groucho-lab module directory) that starts the test server. */
	private static final String TEST_SERVER_SCRIPT = "src/test/resources/scripts/start-lsh-test-server.sh";
	private static final String TEST_SERVER_LOG = "target/lsh-test-server.log";
	private static final long TEST_SERVER_STARTUP_TIMEOUT_MS = 120000;
	private static final long TEST_SERVER_SHUTDOWN_TIMEOUT_MS = 15000;
	private static final long TEST_SERVER_POLL_MS = 500;

	/** Server started by this suite, null if it was already running before the tests (and so must not be stopped). */
	private static Process testServerProcess = null;

	private static final String INSTRUMENTED_METHOD = "instrumentedMethod";

	private static final int MULTIPLE_CHECKS = 5;

	/** Number of UUIDs concatenated in the nonce: the random part must dominate the carved state. */
	private static final int NONCE_UUIDS = 4;

	protected AbstractGovernanceManagerHistory gm;

	/** Makes the "default" state of the single test unique. */
	private String defaultNonce;
	/** Makes the "different" state of the single test unique, independently of the previous one. */
	private String differentNonce;

	/**
	 * @return a new instance of the concrete Governance Manager under test
	 */
	protected abstract AbstractGovernanceManagerHistory createGovernanceManager();

	/**
	 * Points the managers to the server dedicated to the tests, never to the experiment one
	 * (port 8080), and starts it with {@value #TEST_SERVER_SCRIPT} if it is not listening yet.
	 * A server that is already running is used as it is and is not stopped at the end of the suite.
	 * Another address can be given with -Dgroucho.lsh.rest_client.urlBasePath=...: in that case
	 * the server must be started by hand.
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
					+ "] test server already running on " + urlBasePath + ": it will not be stopped at the end of the suite");
			return;
		}
		if (!TEST_SERVER_URL_BASE_PATH.equals(urlBasePath)) {
			Assert.fail("Test groucho-lsh-rest server not reachable on " + urlBasePath
					+ ": with a custom address the server must be started by hand");
		}
		startTestServer();
	}

	@AfterClass
	public static void tearDownTestServer() throws InterruptedException {
		stopTestServer();
	}

	// ------------------------------------------------------------ test server

	private static void startTestServer() throws IOException, InterruptedException {
		// surefire and the IDEs run the tests from the groucho-lab module directory
		File moduleDir = new File(System.getProperty("user.dir"));
		File script = new File(moduleDir, TEST_SERVER_SCRIPT);
		if (!script.isFile()) {
			Assert.fail("Start script of the test server not found: " + script.getAbsolutePath());
		}
		File log = new File(moduleDir, TEST_SERVER_LOG);
		log.getParentFile().mkdirs();

		System.out.println("[" + AbstractGovernanceManagerHistoryTest_IT.class.getSimpleName()
				+ "] starting the test server (" + TEST_SERVER_URL_BASE_PATH + "), log: " + log.getAbsolutePath());
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
				Assert.fail("The test server exited during startup (exit " + exitCode + "), see " + log.getAbsolutePath());
			}
			if (isServerReachable(TEST_SERVER_URL_BASE_PATH)) {
				return;
			}
			Thread.sleep(TEST_SERVER_POLL_MS);
		}
		stopTestServer();
		Assert.fail("The test server did not answer within " + TEST_SERVER_STARTUP_TIMEOUT_MS + " ms, see "
				+ log.getAbsolutePath());
	}

	/** Stops the server only if it was started by this suite. */
	private static void stopTestServer() throws InterruptedException {
		if (testServerProcess == null) {
			return;
		}
		// the script execs java: the started process is the server JVM itself
		testServerProcess.destroy();
		if (!testServerProcess.waitFor(TEST_SERVER_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
			testServerProcess.destroyForcibly().waitFor();
		}
		testServerProcess = null;
	}

	/** Read-only check: without a server every evaluateActivation would return false. */
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
		// no server reset: isolation comes from new states in every test
		this.defaultNonce = randomNonce();
		this.differentNonce = randomNonce();
		GMHistoryDummyInvivoTest.resetInvocations();
		this.gm = this.createGovernanceManager();
	}

	// -------------------------------------------------------------- utilities

	/**
	 * Long random string: the carved state of a test must not look similar (for LSH) to the
	 * ones of previous runs, which would differ only in this part.
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

	/** Two calls within the same test return distinct objects with the same state. */
	protected GMHistoryTestClass createDefaultObject() {
		return new GMHistoryTestClass(1, "defaultLabel_" + this.defaultNonce, true);
	}

	/** State deliberately very different from the default one, so as not to fall into the same LSH bucket. */
	protected GMHistoryTestClass createDifferentObject() {
		return new GMHistoryTestClass(-987654321, "aCompletelyDifferentLabel_" + this.differentNonce, false);
	}

	/** Marks the state of the Context on the server, through the same representation used by the manager. */
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

		Assert.assertTrue("A state never marked must activate the session", condition);
	}

	@Test
	public void markedStateIsNotActivatedTest() throws ClassNotFoundException, LSHException {
		Context context = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, context);

		boolean condition = this.gm.evaluateActivation(context);

		Assert.assertFalse("An already marked state must not activate the session", condition);
	}

	@Test
	public void markedStateIsNotActivatedMultipleChecksTest() throws ClassNotFoundException, LSHException {
		Context context = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, context);

		for (int i = 0; i < MULTIPLE_CHECKS; i++) {
			boolean condition = this.gm.evaluateActivation(context);
			Assert.assertFalse("Check #" + i + ": the marked state must not activate the session", condition);
		}
	}

	@Test
	public void stateMarkedByInvivoSessionIsNotActivatedTest() {
		Context context = this.createContext(this.createDefaultObject());

		boolean firstEvaluation = this.gm.evaluateActivation(context);
		Assert.assertTrue("First evaluation: the state is new", firstEvaluation);

		// the in-vivo session runs the test and, at the end, marks the state just evaluated
		this.gm.doInvivoTestingSession(context);
		Assert.assertEquals("The in-vivo test must have been run once", 1,
				GMHistoryDummyInvivoTest.getInvocations());

		boolean secondEvaluation = this.gm.evaluateActivation(context);
		Assert.assertFalse("After the session the state is known and must not activate it again", secondEvaluation);
	}

	@Test
	public void evaluationDoesNotMarkStateTest() {
		Context context = this.createContext(this.createDefaultObject());

		for (int i = 0; i < MULTIPLE_CHECKS; i++) {
			boolean condition = this.gm.evaluateActivation(context);
			Assert.assertTrue("Check #" + i + ": evaluating alone must not mark the state", condition);
		}
	}

	@Test
	public void equivalentObjectIsNotActivatedTest() throws ClassNotFoundException, LSHException {
		Context markedContext = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, markedContext);

		// different instance with the same values: the history is based on the state, not on identity
		Context equivalentContext = this.createContext(this.createDefaultObject());
		boolean condition = this.gm.evaluateActivation(equivalentContext);

		Assert.assertFalse("An object with the same state must not activate the session", condition);
	}

	@Test
	public void differentStateIsActivatedTest() throws ClassNotFoundException, LSHException {
		Context markedContext = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, markedContext);

		Context differentContext = this.createContext(this.createDifferentObject());
		boolean differentCondition = this.gm.evaluateActivation(differentContext);
		Assert.assertTrue("A state different from the marked ones must activate the session", differentCondition);

		boolean markedCondition = this.gm.evaluateActivation(markedContext);
		Assert.assertFalse("The marked state stays known", markedCondition);
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

		// same Context and same object: the state is carved at every evaluation
		boolean condition = this.gm.evaluateActivation(context);
		Assert.assertTrue("After the change the object is in a new state", condition);
	}

	@Test
	public void historyIsSharedAmongManagerInstancesTest() throws ClassNotFoundException, LSHException {
		Context context = this.createContext(this.createDefaultObject());
		this.markContextState(this.gm, context);

		// the history lives on the server, not in the manager instance
		AbstractGovernanceManagerHistory anotherGm = this.createGovernanceManager();
		boolean condition = anotherGm.evaluateActivation(context);

		Assert.assertFalse("Another manager of the same type must see the already marked state", condition);
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
