/**
 * 
 */
package eu.guna.dice.constraints.operators;

import org.apache.log4j.Logger;

import eu.guna.dice.constraints.BoolNode;
import eu.guna.dice.constraints.exceptions.InvalidTypeException;

/**
 * The logical operators used in the tree representation of logical expressions.
 * 
 * @see net.dice.constraints.BoolNode
 * @author Stefan Guna
 * 
 */
public enum BoolOperator {
	AND {
		public BoolNode joinNodes(BoolNode n1, BoolNode n2) {
			try {
				if (n1.getBool() == false || n1.getBool() == false) {
					log.debug("reducing AND to false");
					return new BoolNode(false);
				}
				if (n1.getBool() == true) {
					log.debug("reducing 'true AND n' to 'n'");
					return n2;
				}
				if (n2.getBool() == true) {
					log.debug("reducing 'n AND true' to 'n'");
					return n1;
				}
			} catch (InvalidTypeException e) {
				// one of n1 and n2 are constants
			}
			return new BoolNode(this, n1, n2);
		}

		public short toMessage() {
			return 0;
		}

		public String toNesc() {
			return "BOOL_AND";
		}

		public String toString() {
			return "&&";
		}
	},
	DBL_IMPLY {
		public BoolNode joinNodes(BoolNode n1, BoolNode n2) {
			log.debug("transforming '<->' to two '->'");
			return AND.joinNodes(IMPLY.joinNodes(n1, n2), IMPLY.joinNodes(n2,
					n1));
		}

		public short toMessage() {
			// TODO Auto-generated method stub
			return 0;
		}

		public String toNesc() {
			return "BOOL_DBL_IMPLY";
		}

		public String toString() {
			return "<->";
		}
	},
	IMPLY {
		public BoolNode joinNodes(BoolNode n1, BoolNode n2) {
			try {
				if (n1.getBool() == false) {
					log.debug("reducing 'false -> n' to true");
					return new BoolNode(true);
				}
				if (n1.getBool() == true) {
					log.debug("reducing 'true -> n' to 'n'");
					return n2;
				}
			} catch (InvalidTypeException e) {
				// n1 is not a constant
			}
			return new BoolNode(this, n1, n2);
		}

		public short toMessage() {
			return 1;
		}

		public String toNesc() {
			return "BOOL_IMPLY";
		}

		public String toString() {
			return "->";
		}
	},
	OR {
		public BoolNode joinNodes(BoolNode n1, BoolNode n2) {
			try {
				if (n1.getBool() == true || n2.getBool() == true) {
					log.debug("reducing OR to true");
					return new BoolNode(true);
				}
				if (n1.getBool() == false) {
					log.debug("reducing 'false OR n' to 'n'");
					return n2;
				}
				if (n2.getBool() == false) {
					log.debug("reducing 'n OR false' to 'n'");
					return n1;
				}
			} catch (InvalidTypeException e) {
				// one of n1 or n2 are not constants
			}
			return new BoolNode(this, n1, n2);
		}

		public short toMessage() {
			return 2;
		}

		public String toNesc() {
			return "BOOL_OR";
		}

		public String toString() {
			return "||";
		}
	};

	public static Logger log = Logger.getLogger(BoolOperator.class);

	/**
	 * Joins two nodes in the tree representation of logical expressions.
	 * 
	 * @see net.dice.constraints.BoolNode
	 * @param n1
	 *            The left hand side of the operator
	 * @param n2
	 *            The right hand side of the operator
	 * @return The result of the join operation.
	 */
	abstract public BoolNode joinNodes(BoolNode n1, BoolNode n2);

	abstract public short toMessage();

	/**
	 * Gets the constant to be written in the generated nesC code.
	 * 
	 * @return The string representation of the constant.
	 */
	abstract public String toNesc();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Enum#toString()
	 */
	@Override
	abstract public String toString();
}
