/**
 * 
 */
package eu.guna.dice.constraints;

/**
 * Represents a literal in the quantification list.
 * 
 * @see net.dice.constraints.Quantifications
 * 
 * @author Stefan Guna
 * 
 */
public class Quantifier {

	/** The quantification type. */
	public enum Type {
		EXISTENTIAL {
			public short toMessage() {
				return 1;
			}

			public String toNesc() {
				return "QUANT_EXISTENTIAL";
			}
		},
		UNIVERSAL {
			public short toMessage() {
				return 0;
			}

			public String toNesc() {
				return "QUANT_UNIVERSAL";
			}
		};

		abstract public short toMessage();

		abstract public String toNesc();
	}

	private int id;

	/** The literal of the quantification. */
	private String name;

	private Type type;

	/**
	 * @param name
	 * @param type
	 */
	protected Quantifier(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Quantifier))
			return false;
		Quantifier other = (Quantifier) obj;
		return name.equals(other.name);
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	protected void setId(int id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		switch (type) {
		case EXISTENTIAL:
			return "E" + name + "(" + id + ") ";
		case UNIVERSAL:
			return "U" + name + "(" + id + ") ";
		}
		return "invalid quantifier";
	}

}
