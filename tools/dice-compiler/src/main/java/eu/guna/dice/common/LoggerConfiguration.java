/**
 * 
 */
package eu.guna.dice.common;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * Class for configuring the logger.
 * 
 * @author Stefan Guna
 * 
 */
public class LoggerConfiguration {

	/**
	 * Configures the logger from the specified file.
	 * 
	 * @param fileName
	 *            The file to load the logger configuration.
	 */
	public static void setupLogging(String fileName) {
		File logProperties = new File(Strings.getString("config-dir"), fileName);
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());
	}
}
