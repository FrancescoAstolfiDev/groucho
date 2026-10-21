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

public class InvivoDemoJsonMain {

	private static final int NUM_INCREMENTI = 10;

	public static void main(String[] args) throws NoSuchMethodException {
		boolean annotato = Contatore.class.getMethod("incrementa_by_properties", int.class)
				.isAnnotationPresent(it.cnr.iasi.saks.groucho.annotation.TestableInVivo.class);
		System.out.println("incrementa_by_properties() annotato dal JSON: " + annotato);

		Contatore contatore = new Contatore();
		for (int delta = 0; delta < NUM_INCREMENTI; delta++) {
			System.out.println("Prima di incrementa_by_properties(" + delta + "): " + contatore.getValore());
			contatore.incrementa_by_properties(delta);
			System.out.println("Dopo incrementa_by_properties(" + delta + "): " + contatore.getValore());
		}
	}
}
