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

import it.cnr.iasi.saks.invivoDemoCommon.CounterUnderTest;

/*
 * Same class as in the invivoDemoByAnnotation demo, but WITHOUT @TestableInVivo:
 * the annotation on incrementa_by_properties() is added at runtime by the
 * groucho-lab agent, which reads src/test/resources/invivoDemoJson/model.json.
 */
public class Counter implements CounterUnderTest {

	private int valore;

	public Counter() {
		this.valore = 0;
	}

	public void incrementa_by_properties(int delta) {
		this.aggiungi(delta);
	}

	public void raddoppia_by_properties() {
		this.raddoppia();
	}

	@Override
	public void aggiungi(int delta) {
		this.valore = this.valore + delta;
	}

	@Override
	public void raddoppia() {
		this.valore = this.valore * 2;
	}

	@Override
	public int getValore() {
		return this.valore;
	}
}
