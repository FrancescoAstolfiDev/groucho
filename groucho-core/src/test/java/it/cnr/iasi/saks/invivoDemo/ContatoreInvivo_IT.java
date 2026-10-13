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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import it.cnr.iasi.saks.groucho.config.PropertyUtil;
import it.cnr.iasi.saks.groucho.tests.util.PropertyUtilNoSingleton;

/**
 * Test d'integrazione: va eseguito da failsafe (suffisso _IT) sulla JVM
 * strumentata da CROCHET con gli agent di groucho-crochet e groucho-core.
 */
public class ContatoreInvivo_IT {

	private static final String GM_CLASSNAME = "it.cnr.iasi.saks.invivoDemo.DemoGovernanceManager";

	@Test
	public void incrementaTestIT() throws IOException {
		PropertyUtilNoSingleton prop = PropertyUtilNoSingleton.getInstance();
		prop.setProperty(PropertyUtil.GOVERNANCE_MANAGER_CLASS_LABEL, GM_CLASSNAME);

		File resultFile = File.createTempFile("invivoDemoResult", ".txt");
		resultFile.deleteOnExit();
		System.setProperty(ContatoreInvivoTest.RESULT_FILE_PROPERTY, resultFile.getAbsolutePath());

		Contatore contatore = new Contatore();
		contatore.incrementa_annotated(5);

		// Il rollback ha annullato le modifiche del test in-vivo:
		// rimane solo l'effetto dell'invocazione reale.
		Assert.assertEquals(5, contatore.getValore());

		try (BufferedReader stream = new BufferedReader(new FileReader(resultFile))) {
			Assert.assertTrue(Boolean.parseBoolean(stream.readLine()));
		}
	}
}
