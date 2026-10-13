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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import it.cnr.iasi.saks.groucho.common.Context;

/**
 * Test in-vivo invocato per riflessione dal Governance Manager: servono una
 * classe pubblica con costruttore senza argomenti e un metodo pubblico con un
 * solo parametro di tipo Context.
 */
public class ContatoreInvivoTest {

	public static final String RESULT_FILE_PROPERTY = "invivoDemo.resultFile";

	public void incrementaInvivoTest(Context c) {
		Contatore contatore = (Contatore) c.getInstrumentedObject();
		List<Object> params = c.getOtherReferencesInContext();
		int delta = (int) params.get(0);
		// Il test opera sullo stato "vivo" dell'oggetto: dopo il rollback di
		// CROCHET queste modifiche verranno annullate.
		// Nota: non invocare qui metodi annotati con @TestableInVivo.
		int prima = contatore.getValore();
		contatore.aggiungi(delta);
		boolean esito = (contatore.getValore() == prima + delta);

		// L'esito sopravvive al rollback solo se salvato fuori dalla heap.
		this.salvaEsito(esito);
	}

	private void salvaEsito(boolean esito) {
		String path = System.getProperty(RESULT_FILE_PROPERTY);
		if (path == null) {
			System.out.println("[ContatoreInvivoTest] esito: " + esito);
			return;
		}
		try (PrintWriter stream = new PrintWriter(new File(path))) {
			stream.println(esito);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
