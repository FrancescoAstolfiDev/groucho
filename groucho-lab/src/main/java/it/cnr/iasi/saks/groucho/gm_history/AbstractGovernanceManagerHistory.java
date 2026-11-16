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

import it.cnr.iasi.saks.groucho.callback.SimpleGovernanceManager;
import it.cnr.iasi.saks.groucho.common.Context;
import it.cnr.iasi.saks.groucho.common.StateCarver;
import it.cnr.iasi.saks.groucho.config.PropertyUtil;
import it.cnr.iasi.saks.groucho.lsh.exceptions.LSHException;
import it.cnr.iasi.saks.groucho.lsh.rest_client.StateObserverRestClientFactory;

/**
 * Governance Manager based on the history of the observed states: the in-vivo
 * session is activated only if the carved state has not been seen before.
 *
 * Subclasses choose how to represent the state and which client to use to
 * query the groucho-lsh-rest server.
 */
public abstract class AbstractGovernanceManagerHistory extends SimpleGovernanceManager {

	private String processedState = null;

	public AbstractGovernanceManagerHistory() {
		super();
	}

	@Override
	public boolean evaluateActivation(Context context) {
		if (context == null) {
			return false;
		}
		try {
			String carvedState = this.carveState(context);
			if (carvedState == null) {
				return false;
			}
			this.processedState = this.processState(carvedState);
			return this.decideActivation(carvedState, this.processedState);
		} catch (Exception e) {
			// server unreachable or error: no in-vivo session
			System.out.println(this.tag() + "server error: " + e);
			return false;
		}
	}

	/**
	 * Captures the state to evaluate. By default it runs the StateCarver on all the fields
	 * of the instrumented object; subclasses may build a different state.
	 *
	 * @return the state, or null if this invocation must not be evaluated
	 */
	protected String carveState(Context context) throws ClassNotFoundException {
		return new StateCarver(context).carveAllFields();
	}

	/**
	 * Decides whether to activate the session for the captured state. By default it
	 * activates it only if the server considers the state new.
	 */
	protected boolean decideActivation(String carvedState, String processedState) throws LSHException {
		boolean isUnknown = this.isStateUnknown(processedState);
		System.out.println(this.tag() + "state: " + carvedState + " -> new: " + isUnknown);
		return isUnknown;
	}

	/**
	 * Creates the factory of the REST clients for the groucho-lsh-rest server. The server address
	 * is read from {@value PropertyUtil#LSH_REST_CLIENT_URL_BASE_PATH_LABEL}, first among the system
	 * properties (-D on the command line) and then in the groucho configuration file; when it is
	 * missing, the default client address is used (http://localhost:8080/groucho-state-observer).
	 */
	protected static StateObserverRestClientFactory createClientFactory() {
		String urlBasePath = System.getProperty(PropertyUtil.LSH_REST_CLIENT_URL_BASE_PATH_LABEL);
		if ((urlBasePath == null) || urlBasePath.trim().isEmpty()) {
			urlBasePath = PropertyUtil.getInstance().getProperty(PropertyUtil.LSH_REST_CLIENT_URL_BASE_PATH_LABEL);
		}
		if ((urlBasePath == null) || urlBasePath.trim().isEmpty()) {
			return new StateObserverRestClientFactory();
		}
		return new StateObserverRestClientFactory(urlBasePath.trim());
	}

	/** @return the prefix of the lines printed by this manager. */
	protected String tag() {
		return "[" + this.getClass().getSimpleName() + "] ";
	}

	@Override
	protected void doInvivoTestingSession(Context context) {
		super.doInvivoTestingSession(context);
		try {
			this.markState(this.processedState);
		} catch (LSHException e) {
			// Not rethrown: an exception here would skip the rollback of the session
			// and leave the other threads paused.
			System.out.println(this.tag() + "state not marked: " + e);
		}
	}

	/**
	 * @param carvedState the state produced by the StateCarver
	 * @return the representation of the state to send to the server
	 */
	protected abstract String processState(String carvedState);

	protected abstract boolean isStateUnknown(String processedState) throws LSHException;

	protected abstract void markState(String processedState) throws LSHException;
}
