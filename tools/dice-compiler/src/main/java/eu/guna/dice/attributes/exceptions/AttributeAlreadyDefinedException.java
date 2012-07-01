/**
 * DICe - Distributed Invariants Checker
 * Monitors a global invariant like 
 * "forall m, n: temperature@m - temperature@n < T"
 * on a wireless sensor network.
 * Copyright (C) 2012 Stefan Guna, svguna@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.guna.dice.attributes.exceptions;

/**
 * This exception is thrown when an attribute already exists.
 * 
 * @author Stefan Guna
 * 
 */
public class AttributeAlreadyDefinedException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * @param attributeName
	 *            The attributeName that already exists.
	 */
	public AttributeAlreadyDefinedException(String attributeName) {
		super("Attribute '" + attributeName + "' already defined");
	}
}
