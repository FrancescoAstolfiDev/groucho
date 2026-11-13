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
package it.cnr.iasi.saks.invivoDemoCommon;

/**
 * Cio' che il test in-vivo comune usa dei due Contatore (versione annotata e
 * versione JSON). E' un'interfaccia e non una superclasse perche' StateCarver
 * legge solo i campi dichiarati nella classe concreta: il campo "valore" deve
 * restare in ciascun Contatore, altrimenti lo stato carvato sarebbe vuoto.
 */
public interface ContatoreSottoTest {

	/*
	 * Logica non strumentata: il test in-vivo deve invocare questa e non il
	 * metodo marcato come in-vivo, altrimenti scatenerebbe una sessione annidata.
	 */
	void aggiungi(int delta);

    void raddoppia();

    int getValore();
}
