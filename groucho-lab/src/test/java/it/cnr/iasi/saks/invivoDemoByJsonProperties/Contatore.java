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
package it.cnr.iasi.saks.invivoDemoByJsonProperties;

import it.cnr.iasi.saks.invivoDemoCommon.ContatoreSottoTest;

/*
 * Stessa classe della demo invivoDemoByAnnotation, ma SENZA @TestableInVivo:
 * l'annotazione su incrementa_by_properties() viene aggiunta a runtime
 * dall'agent di groucho-lab, leggendo src/test/resources/invivoDemoJson/model.json.
 */
public class Contatore implements ContatoreSottoTest {

	private int valore;

	public Contatore() {
		this.valore = 0;
	}

	public void incrementa_by_properties(int delta) {
		this.aggiungi(delta);
	}

	@Override
	public void aggiungi(int delta) {
		this.valore = this.valore + delta;
	}

	@Override
	public int getValore() {
		return this.valore;
	}
}
