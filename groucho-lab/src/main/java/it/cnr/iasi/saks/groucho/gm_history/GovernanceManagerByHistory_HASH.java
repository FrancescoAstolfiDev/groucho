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
 * Sends to the server, through the LSH endpoints, a hash of the carved state computed on the client side.
 */
public class GovernanceManagerByHistory_HASH extends AbstractGovernanceManagerHistory {

	private final StateObserverLSHRestClient client;

	public GovernanceManagerByHistory_HASH() {
		super();
		StateObserverRestClientFactory factory = createClientFactory();
		this.client = (StateObserverLSHRestClient) factory.getStateObserverLSH();
	}

	/**
	 * Computes a 64-bit FNV-1a hash of the state. The value is stable across different
	 * JVMs, which matters because the server stores the seen states on disk, and it
	 * contains only hexadecimal digits, so it can travel in the path of the LSH endpoints.
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
