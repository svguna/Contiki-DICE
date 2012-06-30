/**
 * 
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
