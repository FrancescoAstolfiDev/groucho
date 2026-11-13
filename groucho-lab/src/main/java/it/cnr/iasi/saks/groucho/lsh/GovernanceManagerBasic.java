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
package it.cnr.iasi.saks.groucho.lsh;

import it.cnr.iasi.saks.groucho.lsh.exceptions.LSHException;
import it.cnr.iasi.saks.groucho.lsh.rest_client.StateObserverRestClient;
import it.cnr.iasi.saks.groucho.lsh.rest_client.StateObserverRestClientFactory;

/**
 * Invia al server lo stato carvato cosi' com'e'.
 */
public class GovernanceManagerBasic extends AbstractGovernanceManagerHistory {

	private final StateObserverRestClient client;

	public GovernanceManagerBasic() {
		super();
		StateObserverRestClientFactory factory = new StateObserverRestClientFactory();
		this.client = (StateObserverRestClient) factory.getStateObserver();
	}

	@Override
	protected String processState(String carvedState) {

		return carvedState;
	}

	@Override
	protected boolean isStateUnknown(String processedState) throws LSHException {
		return this.client.isStateUnknown(processedState);
	}

	@Override
	protected void markState(String processedState) throws LSHException {
		this.client.markState(processedState);
	}
}
