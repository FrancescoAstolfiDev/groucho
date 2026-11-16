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

import it.cnr.iasi.saks.groucho.lsh.exceptions.LSHException;
import it.cnr.iasi.saks.groucho.lsh.rest_client.StateObserverLSHRestClient;
import it.cnr.iasi.saks.groucho.lsh.rest_client.StateObserverRestClientFactory;

/**
 * Invia al server, tramite gli endpoint LSH, un hash dello stato carvato calcolato lato client.
 */
public class GovernanceManagerByHistory_HASH extends AbstractGovernanceManagerHistory {

	private final StateObserverLSHRestClient client;

	public GovernanceManagerByHistory_HASH() {
		super();
		StateObserverRestClientFactory factory = createClientFactory();
		this.client = (StateObserverLSHRestClient) factory.getStateObserverLSH();
	}

	/**
	 * Calcola un hash FNV-1a a 64 bit dello stato. Il valore e' stabile fra JVM
	 * diverse, perche' il server conserva su disco gli stati visti, e contiene solo
	 * cifre esadecimali, quindi puo' viaggiare nel path degli endpoint LSH.
	 */
	@Override
	protected String processState(String carvedState) {
		long hash = 0xcbf29ce484222325L;
		for (int index = 0; index < carvedState.length(); index++) {
			hash ^= carvedState.charAt(index);
			hash *= 0x100000001b3L;
		}
		return String.format("%016x", hash);
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
