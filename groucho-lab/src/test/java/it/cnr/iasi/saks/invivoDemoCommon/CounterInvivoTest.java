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
package it.cnr.iasi.saks.invivoDemoCommon;

import it.cnr.iasi.saks.groucho.common.Context;

/**
 * Single set of in-vivo tests on the state of a Counter: the session is activated
 * by a new state, whatever annotated method intercepted it, and it always checks
 * invariants and operations. The changes made here are undone by the CROCHET
 * rollback at the end of the session.
 */
public class CounterInvivoTest {

	private static final int DELTA = 1;

	public void statoInvivoTest(Context c) {
		CounterUnderTest contatore = (CounterUnderTest) c.getInstrumentedObject();
		int statoOsservato = contatore.getValore();

		// Invariants first, because the operations change the observed state.
		report("valoreNonNegativo", valoreNonNegativo(statoOsservato));
		report("statoIntero", statoIntero(contatore));

		report("aggiungi", aggiungi(contatore, statoOsservato));
		// The rollback comes only at the end of the session: raddoppia must start from the observed state.
		contatore.aggiungi(statoOsservato - contatore.getValore());
		report("raddoppia", raddoppia(contatore, statoOsservato));
	}

	private boolean valoreNonNegativo(int valore) {
		return valore >= 0;
	}

	private boolean statoIntero(CounterUnderTest contatore) {
		try {
			return contatore.getClass().getDeclaredField("valore").getType() == int.class;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private boolean aggiungi(CounterUnderTest contatore, int prima) {
		contatore.aggiungi(DELTA);
		return contatore.getValore() == prima + DELTA;
	}

	private boolean raddoppia(CounterUnderTest contatore, int prima) {
		contatore.raddoppia();
		return contatore.getValore() == prima * 2;
	}

	// The outcome survives the rollback only if written outside the heap.
	private void report(String check, boolean esito) {
		System.out.println("[CounterInvivoTest] " + check + ": " + esito);
	}
}
