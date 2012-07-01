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
package eu.guna.dice.attributes;

/**
 * Represents the value to be assigned to an attribute.
 * 
 * @author Stefan Guna
 * 
 */
 class Value {

	/** Value types. */
	protected enum Type {
		/** A constant value is assigned by attribution. */
		CONSTANT,
		/** A dynamic value is obtained by periodically calling a function. */
		DYNAMIC,
		/** A static value is obtained by a single function call. */
		EVENT;
	}

	/** The refresh period for dynamic attributes. */
	private int period;

	/** The type of the value */
	private Type type;

	/** The String representation of the value for constant attributes. */
	private String value;

	/**
	 * Constructor for constant attributes for which the value is assigned by a
	 * function call.
	 */
	protected Value() {
		this.type = Type.EVENT;
	}

	/**
	 * Constructor for dynamic attributes.
	 * 
	 * @param period
	 *            The period in milliseconds to refresh the attribute.
	 */
	protected Value(int period) {
		this.type = Type.DYNAMIC;
		this.period = period;
	}

	/**
	 * Constructor for constant attribute. The value is assigned by attribution.
	 * 
	 * @param value
	 */
	protected Value(String value) {
		this.type = Type.CONSTANT;
		this.value = value;
	}

	protected int getPeriod() {
		return period;
	}

	/**
	 * @return the type
	 */
	protected Type getType() {
		return type;
	}

	/**
	 * @return the value
	 */
	protected String getValue() {
		return value;
	}

	protected boolean isConstant() {
		return type == Type.CONSTANT;
	}

	protected boolean isDynamic() {
		return type == Type.DYNAMIC;
	}

	protected boolean isStatic() {
		return type == Type.EVENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return value;
	}

}
