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
package it.cnr.iasi.saks.groucho.gm_history;

/**
 * Oggetto "instrumentato" usato dai test dei Governance Manager basati sulla storia:
 * il suo stato viene carvato dallo StateCarver e inviato al server groucho-lsh-rest.
 *
 * I campi sono solo primitivi e stringhe, cosi' lo stato carvato con profondita' 1
 * dipende unicamente dai valori impostati dal test.
 */
public class GMHistoryTestClass {

	private int context_to_add;
	private String label;
	private boolean flag;

	public GMHistoryTestClass(int context_to_add, String label, boolean flag) {
		this.context_to_add = context_to_add;
		this.label = label;
		this.flag = flag;
	}

	public int getContextToAdd() {
		return this.context_to_add;
	}

	public void setContextToAdd(int context_to_add) {
		this.context_to_add = context_to_add;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isFlag() {
		return this.flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}
}
