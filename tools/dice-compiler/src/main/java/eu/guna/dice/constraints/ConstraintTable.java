/**
 * 
 */
package eu.guna.dice.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A constraint lookup table used in the constraint compiling process.
 * 
 * @author Stefan Guna
 * 
 */
public class ConstraintTable {
	private static Logger log = Logger.getLogger(ConstraintTable.class);

	/** Constraint aliases */
	private HashMap<String, BoolNode> aliases;

	/** Constraints to be enforced. */
	private ArrayList<BoolNode> constraints;

	protected ConstraintTable() {
		constraints = new ArrayList<BoolNode>();
		aliases = new HashMap<String, BoolNode>();
	}

	/**
	 * Add a new constraint alias.
	 * 
	 * @param name
	 *            The name of the alias.
	 * @param node
	 *            The root of the boolean expression.
	 */
	protected void addAlias(String name, BoolNode node) {
		log.debug("adding alias:" + name + "=" + node);
		aliases.put(name, node);
	}

	/**
	 * Add a new constraint to be enforced with the default time tolerance.
	 * 
	 * @param node
	 *            The root of the boolean expression.
	 */
	protected void addConstraint(BoolNode node) {
		log.debug("adding constraint:" + node);
		constraints.add(node);
	}

	/**
	 * Return a constraint from the alias tabl.
	 * 
	 * @param name
	 *            The name of the alias.
	 * @return The root of the tree representing the constraint.
	 */
	protected BoolNode getConstraintByAlias(String name) {
		BoolNode result = aliases.get(name);
		log.debug("looking up(" + name + ")=" + result);
		return result;
	}

	/**
	 * 
	 * @return the constraint list
	 */
	public List<BoolNode> getConstraints() {
		return constraints;
	}
}
