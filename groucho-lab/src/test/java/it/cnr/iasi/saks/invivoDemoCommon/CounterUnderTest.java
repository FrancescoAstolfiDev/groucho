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
 * What the shared in-vivo test uses of the two Counter classes (annotated version
 * and JSON version). It is an interface and not a superclass because StateCarver
 * reads only the fields declared in the concrete class: the "valore" field must
 * stay in each Counter, otherwise the carved state would be empty.
 */
public interface CounterUnderTest {

	/*
	 * Non-instrumented logic: the in-vivo test must invoke this one and not the
	 * method marked as in-vivo, otherwise it would trigger a nested session.
	 */
	void aggiungi(int delta);

    void raddoppia();

    int getValore();
}
