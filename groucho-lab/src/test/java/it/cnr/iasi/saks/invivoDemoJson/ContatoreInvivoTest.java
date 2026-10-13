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
package it.cnr.iasi.saks.invivoDemoJson;

import java.util.List;

import it.cnr.iasi.saks.groucho.common.Context;

public class ContatoreInvivoTest {

	public void incrementaInvivoTest(Context c) {
		Contatore contatore = (Contatore) c.getInstrumentedObject();
		List<Object> params = c.getOtherReferencesInContext();
		int delta = (int) params.get(0);
		// Il test opera sullo stato "vivo" dell'oggetto: dopo il rollback di
		// CROCHET queste modifiche verranno annullate.
		int prima = contatore.getValore();
		contatore.aggiungi(delta);
		boolean esito = (contatore.getValore() == prima + delta);

		// L'esito sopravvive al rollback solo se scritto fuori dalla heap.
		System.out.println("[ContatoreInvivoTest/JSON] esito: " + esito);
	}
}
