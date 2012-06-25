/**
 * 
 */
package eu.guna.dice.common;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * Loads the string constants from a file.
 * 
 * @author Stefan Guna
 * 
 */
public class Strings {
	public static Logger log = Logger.getLogger(Strings.class);

	/** The file to load strings from */
	private static final String BUNDLE_NAME = "eu.guna.dice.common.strings-conf"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Strings() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			log.error(e.getMessage());
			return '!' + key + '!';
		}
	}
}
