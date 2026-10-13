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

import it.cnr.iasi.saks.groucho.config.PropertyUtil;
import it.cnr.iasi.saks.groucho.tests.util.PropertyUtilNoSingleton;

/**
 * Entry point per eseguire/debuggare la demo fuori da Maven (es. da IntelliJ).
 * La JVM va lanciata con la JRE di CROCHET e con gli stessi argomenti che il
 * pom radice passa a failsafe (-agentpath, -javaagent, -Xbootclasspath/p, ...).
 */
public class InvivoDemoMain {

	private static final String GM_CLASSNAME = "it.cnr.iasi.saks.invivoDemo.DemoGovernanceManager";

	public static void main(String[] args) {
		PropertyUtilNoSingleton prop = PropertyUtilNoSingleton.getInstance();
		prop.setProperty(PropertyUtil.GOVERNANCE_MANAGER_CLASS_LABEL, GM_CLASSNAME);

		Contatore contatore = new Contatore();
		System.out.println("Prima di incrementa(5): " + contatore.getValore());

		contatore.incrementa_annotated(5);

		System.out.println("Dopo incrementa(5): " + contatore.getValore());
	}
}
