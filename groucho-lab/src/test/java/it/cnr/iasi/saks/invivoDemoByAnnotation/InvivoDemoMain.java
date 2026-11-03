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

/**
 * Entry point per eseguire/debuggare la demo fuori da Maven (es. da IntelliJ).
 * La JVM va lanciata con la JRE di CROCHET e con gli stessi argomenti che il
 * pom radice passa a failsafe (-agentpath, -javaagent, -Xbootclasspath/p, ...).
 * Il Governance Manager (LSHGovernanceManager) e' indicato in
 * src/test/resources/invivoDemoAnnotation/invivoDemoAnnotation.properties.
 */
public class InvivoDemoMain {

	private static final int NUM_INCREMENTI = 10;

	public static void main(String[] args) {
		Contatore contatore = new Contatore();
		for (int delta = 0; delta < NUM_INCREMENTI; delta++) {
			System.out.println("Prima di incrementa_annotated(" + delta + "): " + contatore.getValore());
			contatore.incrementa_annotated(delta);
			System.out.println("Dopo incrementa_annotated(" + delta + "): " + contatore.getValore());
//			System.out.println("Prima di raddoppia_annotated(): " + contatore.getValore());
//			contatore.raddoppia_annotated();
//			System.out.println("Dopo raddoppia_annotated(): " + contatore.getValore());
		}
	}
}
