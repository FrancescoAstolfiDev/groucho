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
 * Entry point to run/debug the demo outside Maven (e.g. from IntelliJ).
 * The JVM must be started with the CROCHET JRE and with the same arguments the
 * root pom passes to failsafe (-agentpath, -javaagent, -Xbootclasspath/p, ...).
 * The Governance Manager is set in
 * src/test/resources/invivoDemoAnnotation/invivoDemoAnnotation.properties.
 */
public class InvivoDemoMain {

	private static final int NUM_INCREMENTI = 10;

	public static void main(String[] args) {
		Counter contatore = new Counter();
		for (int delta = 0; delta < NUM_INCREMENTI; delta++) {
			System.out.println("Before incrementa_annotated(" + delta + "): " + contatore.getValore());
			contatore.incrementa_annotated(delta);
			System.out.println("After incrementa_annotated(" + delta + "): " + contatore.getValore());
//			System.out.println("Before raddoppia_annotated(): " + contatore.getValore());
//			contatore.raddoppia_annotated();
//			System.out.println("After raddoppia_annotated(): " + contatore.getValore());
		}
	}
}
