/**
 * 
 */
package eu.guna.dice.constraints.exceptions;

/**
 * The exception thrown when accessing an invalid type.
 * 
 * @author Stefan Guna
 * 
 */
public class InvalidTypeException extends Exception {

	public InvalidTypeException() {
		super("Invalid node type.");
	}

	private static final long serialVersionUID = 1L;

}
