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
package it.cnr.iasi.saks.invivoDemo;

import it.cnr.iasi.saks.groucho.annotation.TestableInVivo;

/**
 * Classe sotto test (SUT): il metodo annotato con @TestableInVivo, quando
 * invocato, puo' attivare una sessione di test in-vivo.
 */
public class Contatore {

	private int valore;

	public Contatore() {
		this.valore = 0;
	}

	@TestableInVivo(invivoTestClass = "it.cnr.iasi.saks.invivoDemo.ContatoreInvivoTest", invivoTest = "incrementaInvivoTest")
	public void incrementa_annotated(int delta) {
		this.aggiungi(delta);
	}

	/*
	 * Logica non annotata: il test in-vivo deve invocare questa e non
	 * incrementa(), altrimenti scatenerebbe una sessione in-vivo annidata.
	 */
	public void aggiungi(int delta) {
		this.valore = this.valore + delta;
	}

	public int getValore() {
		return this.valore;
	}
}
