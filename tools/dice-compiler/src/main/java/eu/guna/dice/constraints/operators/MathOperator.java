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
package eu.guna.dice.constraints.operators;

import org.apache.log4j.Logger;

import eu.guna.dice.constraints.MathNode;
import eu.guna.dice.constraints.Value;
import eu.guna.dice.constraints.exceptions.InvalidTypeException;

/**
 * The mathematical operators used in the tree representation of mathematical
 * expressions.
 * 
 * @see net.dice.constraints.MathNode
 * @author Stefan Guna
 * 
 */
public enum MathOperator {

	DIV {
		@Override
		public MathOperator inverseOperator() {
			return MUL;
		}

		@Override
		public boolean isAdditive() {
			return false;
		}

		@Override
		public boolean isMultiplicative() {
			return true;
		}

		public MathNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "/" + v2);
					return new MathNode(new Value(v1 / v2));
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				if (v1 % v2 == 0) {
					log.debug("reducing " + v1 + "/" + v2);
					return new MathNode(new Value(v1 / v2));
				}
			} catch (InvalidTypeException e) {
				// one of the nodes is not a leaf
			}
			return new MathNode(this, n1, n2);
		}

		public boolean minimizes() {
			return true;
		}

		public short toMessage() {
			return 7;
		}

		public String toContiki() {
			return "MATH_DIV";
		}

		public String toString() {
			return "/";
		}

	},
	MINUS {
		@Override
		public MathOperator inverseOperator() {
			return PLUS;
		}

		@Override
		public boolean isAdditive() {
			return true;
		}

		@Override
		public boolean isMultiplicative() {
			return false;
		}

		public MathNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "-" + v2);
					return new MathNode(new Value(v1 - v2));
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "-" + v2);
				return new MathNode(new Value(v1 - v2));
			} catch (InvalidTypeException e) {
				// one of the nodes is not a leaf
			}
			return new MathNode(this, n1, n2);
		}

		public boolean minimizes() {
			return true;
		}

		@Override
		public short toMessage() {
			return 8;
		}

		public String toContiki() {
			return "MATH_MINUS";
		}

		public String toString() {
			return "-";
		}
	},
	MOD {
		@Override
		public MathOperator inverseOperator() {
			return null;
		}

		@Override
		public boolean isAdditive() {
			return false;
		}

		@Override
		public boolean isMultiplicative() {
			return false;
		}

		public MathNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "%" + v2);
					return new MathNode(new Value(v1 % v2));
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "%" + v2);
				return new MathNode(new Value(v1 % v2));
			} catch (InvalidTypeException e) {
				// one of the nodes is not a leaf
			}
			return new MathNode(this, n1, n2);
		}

		public boolean minimizes() {
			// TODO don't know about mod
			return true;
		}

		@Override
		public short toMessage() {
			return 9;
		}

		public String toContiki() {
			return "MATH_MOD";
		}

		public String toString() {
			return "%";
		}
	},
	MUL {
		@Override
		public MathOperator inverseOperator() {
			return DIV;
		}

		@Override
		public boolean isAdditive() {
			return false;
		}

		@Override
		public boolean isMultiplicative() {
			return true;
		}

		public MathNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "*" + v2);
					return new MathNode(new Value(v1 * v2));
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "*" + v2);
				return new MathNode(new Value(v1 * v2));
			} catch (InvalidTypeException e) {
				// one of the nodes is not a leaf
			}
			return new MathNode(this, n1, n2);
		}

		public boolean minimizes() {
			return false;
		}

		@Override
		public short toMessage() {
			return 10;
		}

		public String toContiki() {
			return "MATH_MUL";
		}

		public String toString() {
			return "*";
		}
	},

	PLUS {
		@Override
		public MathOperator inverseOperator() {
			return MINUS;
		}

		@Override
		public boolean isAdditive() {
			return true;
		}

		@Override
		public boolean isMultiplicative() {
			return false;
		}

		public MathNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "+" + v2);
					return new MathNode(new Value(v1 + v2));
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "+" + v2);
				return new MathNode(new Value(v1 + v2));
			} catch (InvalidTypeException e) {
				// one of the nodes is not a leaf
			}
			return new MathNode(this, n1, n2);
		}

		public boolean minimizes() {
			return false;
		}

		@Override
		public short toMessage() {
			return 11;
		}

		public String toContiki() {
			return "MATH_PLUS";
		}

		public String toString() {
			return "+";
		}
	};

	public static Logger log = Logger.getLogger(MathOperator.class);

	abstract public MathOperator inverseOperator();

	abstract public boolean isAdditive();

	abstract public boolean isMultiplicative();

	abstract public MathNode joinNodes(MathNode n1, MathNode n2);

	/**
	 * Tells whether this operators requires the right hand side to be minimzed
	 * in the aggregation pattern.
	 * 
	 * @return true if the right hand side must be minimized, false otherwise
	 */
	abstract public boolean minimizes();

	abstract public short toMessage();

	/**
	 * Gets the constant to be written in the generated nesC code.
	 * 
	 * @return The string representation of the constant.
	 */
	abstract public String toContiki();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Enum#toString()
	 */
	@Override
	abstract public String toString();
}
