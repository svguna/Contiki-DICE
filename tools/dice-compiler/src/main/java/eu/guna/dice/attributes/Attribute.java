/**
 * 
 */
package eu.guna.dice.attributes;

/**
 * An attribute is a node state variable.
 * 
 * Attributes can be constant or dynamic. Constant attributes have the same
 * value though the entire system lifetime. The value is either assigned by
 * direct attribution or is the result of a function call.
 * 
 * The value of dynamic attributes change during the system lifetime. The change
 * period must be specified. The value of dynamic attributes is set by a
 * function call.
 * 
 * Three types are allowed for attributes: boolean, integer and float.
 * 
 * @author Stefan Guna
 * 
 */
class Attribute {

	/** Attribute types. */
	public enum Type {
		BOOL {
			public String toString() {
				return "bool";
			}
		},
		FLOAT {
			public String toString() {
				return "float";
			}
		},
		INT {
			public String toString() {
				return "uint16_t";
			}
		};

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public abstract String toString();
	};

	/** The name of the attribute. */
	private String name;

	/** The type of the attribute. */
	private Type type;

	/** The value of the attribute. */
	private Value value;

	/**
	 * Constructor of the attribute class.
	 * 
	 * @param name
	 *            The name of the attribute.
	 * @param type
	 *            The type of the attribute.
	 * @param value
	 *            The value of the attribute.
	 */
	protected Attribute(String name, Type type, Value value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	/**
	 * Getter for the name of the attribute.
	 * 
	 * @return the name of the attribute.
	 */
	protected String getName() {
		return name;
	}

	/**
	 * Getter for the type of the attribute.
	 * 
	 * @return the type of the attribute.
	 */
	protected Type getType() {
		return type;
	}

	/**
	 * Getter for the value of the attribute.
	 * 
	 * @return the value of the attribute
	 */
	protected Value getValue() {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type + " ");
		buf.append(name);
		if (value.getType() == Value.Type.CONSTANT)
			buf.append(" = " + value);
		if (value.getType() == Value.Type.DYNAMIC) {
			buf.append(", new_");
			buf.append(name);
		}
		buf.append(";");
		return buf.toString();
	}
}
