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
 * Set unico di test in-vivo sullo stato di un Contatore: la sessione viene attivata
 * da uno stato nuovo, qualunque sia il metodo annotato che lo ha intercettato, e
 * verifica sempre invarianti e operazioni. Le modifiche fatte qui vengono annullate
 * dal rollback di CROCHET a fine sessione.
 */
public class ContatoreInvivoTest {

	private static final int DELTA = 1;

	public void statoInvivoTest(Context c) {
		ContatoreSottoTest contatore = (ContatoreSottoTest) c.getInstrumentedObject();
		int statoOsservato = contatore.getValore();

		// Prima gli invarianti, perche' le operazioni modificano lo stato osservato.
		report("valoreNonNegativo", valoreNonNegativo(statoOsservato));
		report("statoIntero", statoIntero(contatore));

		report("aggiungi", aggiungi(contatore, statoOsservato));
		// Il rollback arriva solo a fine sessione: raddoppia deve partire dallo stato osservato.
		contatore.aggiungi(statoOsservato - contatore.getValore());
		report("raddoppia", raddoppia(contatore, statoOsservato));
	}

	private boolean valoreNonNegativo(int valore) {
		return valore >= 0;
	}

	private boolean statoIntero(ContatoreSottoTest contatore) {
		try {
			return contatore.getClass().getDeclaredField("valore").getType() == int.class;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private boolean aggiungi(ContatoreSottoTest contatore, int prima) {
		contatore.aggiungi(DELTA);
		return contatore.getValore() == prima + DELTA;
	}

	private boolean raddoppia(ContatoreSottoTest contatore, int prima) {
		contatore.raddoppia();
		return contatore.getValore() == prima * 2;
	}

	// L'esito sopravvive al rollback solo se scritto fuori dalla heap.
	private void report(String check, boolean esito) {
		System.out.println("[ContatoreInvivoTest] " + check + ": " + esito);
	}
}
