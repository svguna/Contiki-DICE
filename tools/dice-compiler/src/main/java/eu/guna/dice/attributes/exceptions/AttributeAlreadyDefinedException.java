/**
 * 
 */
package eu.guna.dice.attributes.exceptions;

/**
 * This exception is thrown when an attribute already exists.
 * 
 * @author Stefan Guna
 * 
 */
public class AttributeAlreadyDefinedException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * @param attributeName
	 *            The attributeName that already exists.
	 */
	public AttributeAlreadyDefinedException(String attributeName) {
		super("Attribute '" + attributeName + "' already defined");
	}
}
