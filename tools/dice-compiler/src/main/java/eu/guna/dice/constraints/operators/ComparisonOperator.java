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

import eu.guna.dice.constraints.BoolNode;
import eu.guna.dice.constraints.MathNode;
import eu.guna.dice.constraints.Value;
import eu.guna.dice.constraints.exceptions.InvalidTypeException;

/**
 * The comparison operators used by the leaves of the tree representation of
 * logical expressions.
 * 
 * @see net.dice.constraints.BoolNode
 * @author Stefan Guna
 * 
 */
public enum ComparisonOperator {
	DIFFERENT {
		@Override
		public BoolNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "!=" + v2);
					return new BoolNode(v1 != v2);
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "!=" + v2);
				return new BoolNode(v1 != v2);
			} catch (InvalidTypeException e) {
				// one of the nodes are not constant
			}
			return normalize(this, n1, n2);
		}

		@Override
		public ComparisonOperator negated() {
			return DIFFERENT;
		}

		@Override
		public String toContiki() {
			return "COMP_DIFFERENT";
		}

		@Override
		public String toString() {
			return "!=";
		}

		@Override
		public short toMessage() {
			return 3;
		}
	},
	EQUAL {
		@Override
		public BoolNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "=" + v2);
					return new BoolNode(v1 == v2);
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "=" + v2);
				return new BoolNode(v1 == v2);
			} catch (InvalidTypeException e) {
				// one of the nodes are not constant
			}
			return normalize(this, n1, n2);
		}

		@Override
		public ComparisonOperator negated() {
			return EQUAL;
		}

		@Override
		public String toContiki() {
			return "COMP_EQUAL";
		}

		@Override
		public String toString() {
			return "=";
		}

		@Override
		public short toMessage() {
			return 4;
		}
	},
	GREATER {
		@Override
		public BoolNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + ">" + v2);
					return new BoolNode(v1 > v2);
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + ">" + v2);
				return new BoolNode(v1 > v2);
			} catch (InvalidTypeException e) {
				// one of the nodes are not constant
			}
			return normalize(this, n1, n2);
		}

		@Override
		public ComparisonOperator negated() {
			return LOWER;
		}

		@Override
		public String toContiki() {
			return "COMP_GREATER";
		}

		@Override
		public String toString() {
			return ">";
		}

		@Override
		public short toMessage() {
			return 5;
		}
	},
	LOWER {
		@Override
		public BoolNode joinNodes(MathNode n1, MathNode n2) {
			try {
				if (Value.runFloat(n1, n2)) {
					float v1 = n1.getFloat(), v2 = n2.getFloat();
					log.debug("reducing " + v1 + "<" + v2);
					return new BoolNode(v1 < v2);
				}
				int v1 = n1.getInt(), v2 = n2.getInt();
				log.debug("reducing " + v1 + "<" + v2);
				return new BoolNode(v1 < v2);
			} catch (InvalidTypeException e) {
				// one of the nodes are not constant
			}
			return normalize(this, n1, n2);
		}

		@Override
		public ComparisonOperator negated() {
			return GREATER;
		}

		@Override
		public String toContiki() {
			return "COMP_LOWER";
		}

		@Override
		public String toString() {
			return "<";
		}

		@Override
		public short toMessage() {
			return 6;
		}
	};

	public static Logger log = Logger.getLogger(ComparisonOperator.class);

	private static BoolNode normalize(ComparisonOperator op, MathNode n1,
			MathNode n2) {
		int left_depth = n1.depth(), right_depth = n2.depth();
		if (left_depth == 1 && right_depth == 0)
			return new BoolNode(op, n1, n2);
		if (left_depth == 0 && right_depth == 1)
			return new BoolNode(op.negated(), n2, n1);
		MathNode left = MathOperator.MINUS.joinNodes(n1, n2);
		MathNode right = new MathNode(new Value(0));
		return new BoolNode(op, left, right);
	}

	abstract public BoolNode joinNodes(MathNode n1, MathNode n2);

	/**
	 * Returns the inverse operator (i.e. for lower, returns greater).
	 * 
	 * @return The inverse operator.
	 */
	abstract public ComparisonOperator negated();

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

	abstract public short toMessage();
}
