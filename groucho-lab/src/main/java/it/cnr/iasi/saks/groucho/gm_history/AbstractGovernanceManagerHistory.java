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
 * Governance Manager basato sulla storia degli stati osservati: la sessione
 * in-vivo viene attivata solo se lo stato carvato non risulta gia' visto.
 *
 * Le sottoclassi scelgono come rappresentare lo stato e con quale client
 * interrogare il server groucho-lsh-rest.
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
			// server non raggiungibile o errore: nessuna sessione in-vivo
			System.out.println(this.tag() + "errore verso il server: " + e);
			return false;
		}
	}

	/**
	 * Cattura lo stato da valutare. Di default usa lo StateCarver su tutti i campi
	 * dell'oggetto instrumentato; le sottoclassi possono costruire uno stato diverso.
	 *
	 * @return lo stato, oppure null se questa chiamata non va valutata
	 */
	protected String carveState(Context context) throws ClassNotFoundException {
		return new StateCarver(context).carveAllFields();
	}

	/**
	 * Decide se attivare la sessione per lo stato catturato. Di default la attiva
	 * solo se il server considera lo stato nuovo.
	 */
	protected boolean decideActivation(String carvedState, String processedState) throws LSHException {
		boolean isUnknown = this.isStateUnknown(processedState);
		System.out.println(this.tag() + "stato: " + carvedState + " -> nuovo: " + isUnknown);
		return isUnknown;
	}

	/**
	 * Crea la factory dei client REST verso il server groucho-lsh-rest. L'indirizzo del server
	 * e' letto da {@value PropertyUtil#LSH_REST_CLIENT_URL_BASE_PATH_LABEL}, prima fra le proprieta'
	 * di sistema (-D sulla riga di comando) e poi nel file di configurazione di groucho; se assente
	 * si usa l'indirizzo predefinito del client (http://localhost:8080/groucho-state-observer).
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

	/** @return il prefisso delle righe stampate da questo manager. */
	protected String tag() {
		return "[" + this.getClass().getSimpleName() + "] ";
	}

	@Override
	protected void doInvivoTestingSession(Context context) {
		super.doInvivoTestingSession(context);
		try {
			this.markState(this.processedState);
		} catch (LSHException e) {
			// Non si rilancia: un'eccezione qui salterebbe il rollback della sessione
			// e lascerebbe in pausa gli altri thread.
			System.out.println(this.tag() + "stato non registrato: " + e);
		}
	}

	/**
	 * @param carvedState lo stato prodotto dallo StateCarver
	 * @return la rappresentazione dello stato da inviare al server
	 */
	protected abstract String processState(String carvedState);

	protected abstract boolean isStateUnknown(String processedState) throws LSHException;

	protected abstract void markState(String processedState) throws LSHException;
}
