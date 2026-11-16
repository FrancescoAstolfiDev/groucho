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
package it.cnr.iasi.saks.invivoDemoByAnnotation;

import it.cnr.iasi.saks.groucho.annotation.TestableInVivo;
import it.cnr.iasi.saks.invivoDemoCommon.CounterUnderTest;

/**
 * Class under test (SUT): the method annotated with @TestableInVivo, when
 * invoked, may activate an in-vivo testing session.
 */
public class Counter implements CounterUnderTest {

	private int valore;

	public Counter() {
		this.valore = 0;
	}

	@TestableInVivo(invivoTestClass = "it.cnr.iasi.saks.invivoDemoCommon.CounterInvivoTest", invivoTest = "statoInvivoTest")
	public void incrementa_annotated(int delta) {
		this.aggiungi(delta);
	}

	@TestableInVivo(invivoTestClass = "it.cnr.iasi.saks.invivoDemoCommon.CounterInvivoTest", invivoTest = "statoInvivoTest")
	public void raddoppia_annotated() {
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
