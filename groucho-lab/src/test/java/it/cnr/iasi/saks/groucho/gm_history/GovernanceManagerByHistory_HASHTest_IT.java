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
 * Runs the {@link AbstractGovernanceManagerHistoryTest_IT} suite on
 * {@link GovernanceManagerByHistory_HASH}, which sends a 64-bit FNV-1a hash of the carved state
 * to the LSH endpoints. It also checks the properties of the hash.
 */
public class GovernanceManagerByHistory_HASHTest_IT extends AbstractGovernanceManagerHistoryTest_IT {

	private static final String HEX_64_BIT_REGEX = "[0-9a-f]{16}";

	@Override
	protected AbstractGovernanceManagerHistory createGovernanceManager() {
		return new GovernanceManagerByHistory_HASH();
	}

	@Test
	public void processStateKnownVectorsTest() {
		// reference vectors of the 64-bit FNV-1a algorithm
		Assert.assertEquals("cbf29ce484222325", this.gm.processState(""));
		Assert.assertEquals("af63dc4c8601ec8c", this.gm.processState("a"));
		Assert.assertEquals("85944171f73967e8", this.gm.processState("foobar"));
	}

	@Test
	public void processStateIsHex64BitTest() throws ClassNotFoundException {
		String carvedState = new StateCarver(this.createContext(this.createDefaultObject())).carveAllFields();

		String processedState = this.gm.processState(carvedState);

		Assert.assertTrue("Invalid hash: " + processedState, processedState.matches(HEX_64_BIT_REGEX));
	}

	@Test
	public void differentStatesHaveDifferentHashesTest() throws ClassNotFoundException {
		String carvedState = new StateCarver(this.createContext(this.createDefaultObject())).carveAllFields();
		String differentCarvedState = new StateCarver(this.createContext(this.createDifferentObject())).carveAllFields();

		Assert.assertNotEquals(this.gm.processState(carvedState), this.gm.processState(differentCarvedState));
	}
}
