/**
 * 
 */
package eu.guna.dice.constraints.exceptions;

/**
 * @author Stefan Guna
 * 
 */
public class QuantifierNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public QuantifierNotFoundException(String quantifierString) {
		super("Quantifier " + quantifierString + " not found.");
	}
}