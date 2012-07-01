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
package eu.guna.dice.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A constraint lookup table used in the constraint compiling process.
 * 
 * @author Stefan Guna
 * 
 */
public class ConstraintTable {
	private static Logger log = Logger.getLogger(ConstraintTable.class);

	/** Constraint aliases */
	private HashMap<String, BoolNode> aliases;

	/** Constraints to be enforced. */
	private ArrayList<BoolNode> constraints;

	protected ConstraintTable() {
		constraints = new ArrayList<BoolNode>();
		aliases = new HashMap<String, BoolNode>();
	}

	/**
	 * Add a new constraint alias.
	 * 
	 * @param name
	 *            The name of the alias.
	 * @param node
	 *            The root of the boolean expression.
	 */
	protected void addAlias(String name, BoolNode node) {
		log.debug("adding alias:" + name + "=" + node);
		aliases.put(name, node);
	}

	/**
	 * Add a new constraint to be enforced with the default time tolerance.
	 * 
	 * @param node
	 *            The root of the boolean expression.
	 */
	protected void addConstraint(BoolNode node) {
		if (constraints.size() == 1) {
			throw new UnsupportedOperationException(
					"This version of the compiler only supports one invariant!");
		}
		log.debug("adding constraint:" + node);
		constraints.add(node);
	}

	/**
	 * Return a constraint from the alias tabl.
	 * 
	 * @param name
	 *            The name of the alias.
	 * @return The root of the tree representing the constraint.
	 */
	protected BoolNode getConstraintByAlias(String name) {
		BoolNode result = aliases.get(name);
		log.debug("looking up(" + name + ")=" + result);
		return result;
	}

	/**
	 * 
	 * @return the constraint list
	 */
	public List<BoolNode> getConstraints() {
		return constraints;
	}
}
