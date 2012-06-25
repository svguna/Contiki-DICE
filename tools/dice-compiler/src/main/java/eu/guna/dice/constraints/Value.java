/**
 * 
 */
package eu.guna.dice.constraints;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import eu.guna.dice.constraints.exceptions.InvalidTypeException;
import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;

/**
 * This class represents a value held by a constant (leaf) in the expression
 * represented as a tree. This can be used by either the mathematical tree or
 * the logical formula tree.
 * 
 * The the following are valid as values: node attributes, boolean constants,
 * float constants and integer constants. Depending on the type, only the
 * appropriate members and methods are valid.
 * 
 * @see net.dice.constraints.BoolNode
 * @see net.dice.constraints.MathNode
 * @author Stefan Guna
 * 
 */
public class Value {

	/**
	 * The type of the value. Can be a node attribute, a boolean constant, a
	 * float constant or a integer constant.
	 */
	protected enum Type {
		ATTRIBUTE, BOOL, FLOAT, INT;
	}

	/**
	 * Tells whether the expression involving two math nodes must be computed as
	 * float.
	 * 
	 * @param n1
	 * @param n2
	 * @return true if the value of any math node is a float, false otherwise
	 * @throws InvalidTypeException
	 */
	public static boolean runFloat(MathNode n1, MathNode n2)
			throws InvalidTypeException {
		if (n1.getValue().getType() == Value.Type.FLOAT
				|| n1.getValue().getType() == Value.Type.FLOAT)
			return true;
		return false;
	}

	private Attribute att_value;

	private boolean bool_value;

	private float float_value;

	private int int_value;

	private Type type;

	/**
	 * @param att_value
	 */
	protected Value(Attribute att_value) {
		type = Type.ATTRIBUTE;
		this.att_value = att_value;
	}

	/**
	 * @param bool_value
	 */
	protected Value(boolean bool_value) {
		type = Type.BOOL;
		this.bool_value = bool_value;
	}

	/**
	 * @param float_value
	 */
	public Value(float float_value) {
		type = Type.FLOAT;
		this.float_value = float_value;
		int_value = (int) float_value;
		// hack: we prefer integers
		if ((float) int_value == float_value)
			type = Type.INT;
	}

	/**
	 * @param int_value
	 */
	public Value(int int_value) {
		type = Type.INT;
		this.int_value = int_value;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *            The value to copy data from.
	 */
	protected Value(Value other) {
		if (other.att_value != null)
			att_value = new Attribute(other.att_value);
		bool_value = other.bool_value;
		float_value = other.float_value;
		int_value = other.int_value;
		type = other.type;
	}

	public int depth() {
		if (type == Type.ATTRIBUTE)
			return 1;
		return 0;
	}

	/** Compare by value. */
	public boolean equals(Value other) {
		if (type != other.type)
			return false;
		switch (type) {
		case ATTRIBUTE:
			return att_value.equals(other.att_value);
		case BOOL:
			return bool_value == other.bool_value;
		case FLOAT:
			return float_value == other.float_value;
		case INT:
			return int_value == other.int_value;
		}
		return false;
	}

	/**
	 * @return the att_value
	 */
	protected Attribute getAttribute() {
		return att_value;
	}

	/**
	 * @return the bool_value
	 * @throws InvalidTypeException
	 */
	protected boolean getBool_value() throws InvalidTypeException {
		if (type != Type.BOOL)
			throw new InvalidTypeException();
		return bool_value;
	}

	/**
	 * @return the float_value
	 * @throws InvalidTypeException
	 */
	protected float getFloat_value() throws InvalidTypeException {
		if (type != Type.FLOAT)
			throw new InvalidTypeException();
		return float_value;
	}

	/**
	 * @return the int_value
	 * @throws InvalidTypeException
	 */
	protected int getInt_value() throws InvalidTypeException {
		if (type != Type.INT)
			throw new InvalidTypeException();
		return int_value;
	}

	/**
	 * Adds this attribute to the pattern, if it has not been added already.
	 * 
	 * @param maxHistory
	 *            The history of quantification. This argument is changed.
	 * @param minHistory
	 *            The history of quantification. This argument is changed.
	 * @param maximization
	 *            The maximization count for attributes. This argument is
	 *            changed and is part of the actual result.
	 * @param minimization
	 *            The minimization count for attributes. This argument is
	 *            changed and is part of the actual result.
	 * @param minimize
	 *            Inverses from maximization to minimization.
	 * @param quantifiers
	 *            The list of "externalized" quantifiers.
	 * @param coefficient
	 */
	protected void getPattern(HashMap<String, List<Integer>> maxHistory,
			HashMap<String, List<Integer>> minHistory, List<Pattern> patterns,
			boolean minimize, Set<Quantifier> quantifiers, int coefficient) {
		if (type != Type.ATTRIBUTE)
			return;
		att_value.getPattern(maxHistory, minHistory, patterns, minimize,
				quantifiers, coefficient);
	}

	/**
	 * @return the type
	 */
	protected Type getType() {
		return type;
	}

	/**
	 * @param int_value
	 *            the int_value to set
	 * @throws InvalidTypeException
	 */
	protected void setInt_value(int int_value) throws InvalidTypeException {
		if (type != Type.INT)
			throw new InvalidTypeException();
		this.int_value = int_value;
	}

	/**
	 * If this value is an attribute, sets the ID of the quantifier.
	 * 
	 * @param quantifiers
	 *            The hashmap (quantifier name; quantifiers)
	 * @throws QuantifierNotFoundException
	 */
	protected void setQuantifier(HashMap<String, Quantifier> quantifiers)
			throws QuantifierNotFoundException {
		if (type != Type.ATTRIBUTE)
			return;
		att_value.setQuantifier(quantifiers);
	}

	/**
	 * Appends the assignment code for this value into the buffer.
	 * 
	 * @param buf
	 *            The buffer to append to
	 * @param index
	 *            The current node index
	 * @return the next node index
	 */
	protected int toNesc(StringBuffer buf, int index) {
		switch (type) {
		case ATTRIBUTE:
			return att_value.toNesc(buf, index);
		case BOOL:
			buf.append("    constraint->nodes[" + index + "].type = BOOL;\n");
			buf.append("    constraint->nodes[" + index
					+ "].data.bool_value = "
					+ ((bool_value) ? "TRUE" : "FALSE") + ";\n");
			return index + 1;
		case FLOAT:
			buf.append("    constraint->nodes[" + index + "].type = FLOAT;\n");
			buf.append("    constraint->nodes[" + index
					+ "].data.float_value = " + float_value + ";\n");
			return index + 1;
		case INT:
			buf.append("    constraint->nodes[" + index + "].type = INT;\n");
			buf.append("    constraint->nodes[" + index + "].data.int_value = "
					+ int_value + ";\n");
			return index + 1;
		}
		return index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		switch (type) {
		case ATTRIBUTE:
			return att_value.toString();
		case BOOL:
			return (bool_value) ? "true" : "false";
		case FLOAT:
			return new Float(float_value).toString();
		case INT:
			return new Integer(int_value).toString();
		}
		return "invalid value";
	}
}
