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

import java.util.concurrent.atomic.AtomicInteger;

import it.cnr.iasi.saks.groucho.common.Context;

/**
 * Dummy in-vivo test invoked by reflection by the SimpleGovernanceManager.
 * It counts its invocations, so the tests can check that the session has been run.
 */
public class GMHistoryDummyInvivoTest {

	public static final String INVIVO_TEST_METHOD = "invivoTestMethod";

	private static final AtomicInteger INVOCATIONS = new AtomicInteger(0);

	public void invivoTestMethod(Context c) {
		INVOCATIONS.incrementAndGet();
	}

	public static int getInvocations() {
		return INVOCATIONS.get();
	}

	public static void resetInvocations() {
		INVOCATIONS.set(0);
	}
}
