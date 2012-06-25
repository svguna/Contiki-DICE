/**
 * 
 */
package eu.guna.dice.constraints.exceptions;

import eu.guna.dice.constraints.Quantifier;

public class QuantifierInUseException extends Error {
	private static final long serialVersionUID = 1L;

	public QuantifierInUseException(Quantifier quantifier) {
		super("Quantifier " + quantifier.getName() + " already used.");
	}
}