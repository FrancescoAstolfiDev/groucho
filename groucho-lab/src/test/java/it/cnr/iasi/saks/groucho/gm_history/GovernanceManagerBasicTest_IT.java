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

import org.junit.Assert;
import org.junit.Test;

import it.cnr.iasi.saks.groucho.common.StateCarver;

/**
 * Esegue la suite di {@link AbstractGovernanceManagerHistoryTest_IT} su {@link GovernanceManagerBasic},
 * che invia al server lo stato carvato cosi' com'e'.
 */
public class GovernanceManagerBasicTest_IT extends AbstractGovernanceManagerHistoryTest_IT {

	@Override
	protected AbstractGovernanceManagerHistory createGovernanceManager() {
		return new GovernanceManagerBasic();
	}

	@Test
	public void processStateIsIdentityTest() throws ClassNotFoundException {
		String carvedState = new StateCarver(this.createContext(this.createDefaultObject())).carveAllFields();

		String processedState = this.gm.processState(carvedState);

		Assert.assertEquals(carvedState, processedState);
	}
}
