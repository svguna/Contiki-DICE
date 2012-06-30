/**
 * 
 */
package eu.guna.dice.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;

/**
 * An attribute contained in a constraint (either in a MathNode or directly in a
 * BoolNode).
 * 
 * @see net.dice.constraints.BoolNode
 * @see net.dice.constraints.MathNode
 * @author Stefan Guna
 */
public class Attribute {

	private static Logger log = Logger.getLogger(Attribute.class);

	/** The hash of the attribute name. */
	private int hash;

	/** The attribute name. */
	private String name;

	/**
	 * The quantifiers used by this attribute.
	 */
	private Quantifier quantifier;

	/**
	 * The node which holds this attributes. This is the string following the
	 * '@' operator in the constraint string.
	 */
	private String quantifierString;

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *            The object to copy data from.
	 */
	protected Attribute(Attribute other) {
		hash = other.hash;
		name = other.name;
		quantifier = other.quantifier;
		quantifierString = other.quantifierString;
	}

	/**
	 * Construct for attributes.
	 * 
	 * @param att_name
	 *            The name of the attribute (the part before '@').
	 * @param att_quantifier
	 *            The node holding the attribute (the part after '@').
	 */
	protected Attribute(String att_name, String att_quantifier) {
		this.hash = att_name.hashCode() & 0xFFFF;
		this.name = att_name;
		this.quantifierString = att_quantifier;
	}

	/** Compare by value */
	public boolean equals(Attribute other) {
		return name.equals(other.name)
				&& quantifierString.equals(other.quantifierString);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Attribute))
			return false;
		Attribute other = (Attribute) obj;
		return name.equals(other.name);
	}

	/**
	 * @return the att_hash
	 */
	public int getHash() {
		return hash;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds a this attribute to the scopingPattern, if it has not been added
	 * already.
	 * 
	 * @param maxHistory
	 *            The history of quantification. This argument is changed.
	 * @param maxHistory
	 *            The history of quantification. This argument is changed.
	 * @param patterns
	 *            The patterns for attributes. This argument is changed and is
	 *            part of the actual result.
	 * @param minimize
	 *            Inverses from maximization to minimization.
	 * @param quantifiers
	 *            The list of "externalized" quantifiers.
	 * @param coefficient
	 */
	protected void getPattern(HashMap<String, List<Integer>> maxHistory,
			HashMap<String, List<Integer>> minHistory, List<Pattern> patterns,
			boolean minimize, Set<Quantifier> quantifiers, int coefficient) {

		if (quantifier.getType() == Quantifier.Type.EXISTENTIAL)
			minimize = !minimize;

		HashMap<String, List<Integer>> history = maxHistory;
		Pattern.Objective objective = Pattern.Objective.MAXIMIZE;
		if (minimize) {
			history = minHistory;
			objective = Pattern.Objective.MINIMIZE;
		}

		List<Integer> myHistory = history.get(name);
		if (myHistory == null) {
			myHistory = new ArrayList<Integer>();
			history.put(name, myHistory);
		}

		quantifiers.add(quantifier);

		if (myHistory.contains(new Integer(quantifier.getId())))
			return;

		Pattern.addPattern(patterns, new Pattern(this, quantifier, objective,
				coefficient));
	}

	/**
	 * @return the att_quantifier
	 */
	protected Quantifier getQuantifier() {
		return quantifier;
	}

	/**
	 * Gets the special scopingPattern type used for scoping.
	 * 
	 * @param boolNodeId
	 */
	protected Pattern getScopingPattern(int implicationId, int boolNodeId) {
		return new Pattern(this, quantifier, implicationId, boolNodeId);
	}

	/**
	 * Set the quantifier id for this attribute.
	 * 
	 * @param ids
	 *            The hashmap (quantifier name; id)
	 * @throws QuantifierNotFoundException
	 */
	protected void setQuantifier(HashMap<String, Quantifier> quantifiers)
			throws QuantifierNotFoundException {
		if (quantifiers.get(quantifierString) == null) {
			log.error("Quantifier " + quantifierString + " not found.");
			throw new QuantifierNotFoundException(quantifierString);
		} else
			quantifier = quantifiers.get(quantifierString);
	}

	/**
	 * Appends the assignment code for this attribute into the buffer.
	 * 
	 * @param buf
	 *            The buffer to append to
	 * @param index
	 *            The current node index
	 * @return the next node index
	 */
	protected int toContiki(StringBuffer buf, int index) {
		buf.append("        { .type = ATTRIBUTE,\n");
		buf.append("          .data.attribute.hash = " + hash + ", /* " + name
				+ " */\n");
		buf.append("          .data.attribute.quantifier = "
				+ quantifier.getId() + ", /* " + quantifier.getName() + " */\n");
		return index + 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (quantifier != null)
			return name + "(" + hash + ")@" + quantifierString + "("
					+ quantifier.getId() + ")";
		return name + "(" + hash + ")@" + quantifierString + "(-)";
	}
}