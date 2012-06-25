/**
 * 
 */
package eu.guna.dice.constraints;

import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * Quantification list used by logical expressions. It contains all the
 * quantifications in the expression. The quantifications are represented by
 * {@link #net.dice.constraints.Quantification} which hold a literal
 * representing the quantified node.
 * 
 * @author Stefan Guna
 * 
 */
class Quantifications {

	private static Logger log = Logger.getLogger(Quantifications.class);

	/** The quantifications list. */
	private Vector<Quantifier> data;

	/** Default constructor. */
	protected Quantifications() {
		data = new Vector<Quantifier>();
	}

	/**
	 * Copy constructor
	 * 
	 * @param other
	 *            The object to copy data from.
	 */
	protected Quantifications(Quantifications other) {
		if (other == null)
			data = new Vector<Quantifier>();
		else
			data = new Vector<Quantifier>(other.data);
	}

	/**
	 * 
	 * @param name
	 * @param type
	 */
	protected Quantifications(String name, Quantifier.Type type) {
		data = new Vector<Quantifier>();
		data.add(new Quantifier(name, type));
	}

	/**
	 * Adds a new quantification to the list.
	 * 
	 * @param name
	 * @param type
	 */
	protected void add(String name, Quantifier.Type type) {
		Quantifier e = new Quantifier(name, type);
		// TODO throw exception
		if (!data.contains(e))
			data.add(e);
	}

	/**
	 * @return the data
	 */
	protected Vector<Quantifier> getQuantifiers() {
		return data;
	}

	/**
	 * Merges this quantification set with another quantification set.
	 * 
	 * @param other
	 */
	protected void merge(Quantifications other) {
		if (other == null)
			return;
		Vector<Quantifier> temp = new Vector<Quantifier>(
				data);
		temp.retainAll(other.data);
		// TODO throw exception
		if (temp.isEmpty())
			data.addAll(other.data);
		else
			log.error("quantifications intersect in: " + temp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return data.toString();
	}

}
