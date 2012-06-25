/**
 * 
 */
package eu.guna.dice.attributes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;


import eu.guna.dice.common.LoggerConfiguration;
import eu.guna.dice.common.Strings;

/**
 * The attribute specification compiler.
 * 
 * @author Stefan Guna
 * 
 */
public class AttributesCompiler {
	public static Logger log = Logger.getLogger(AttributesCompiler.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LoggerConfiguration.setupLogging(Strings
				.getString("Attributes.logger-configuration"));
		parse(Strings.getString("Attributes.attribute-file"));
	}

	/**
	 * Preprocess the attribute input file. The result is the TinyOS interfaces
	 * and modules that are linked with the application and the DiCE runtime.
	 * 
	 * @param inputFile
	 *            the attribute input file
	 */
	public static void parse(String inputFile) {
		attributeParser parser;
		try {
			attributeLexer lexer = new attributeLexer(new ANTLRFileStream(
					inputFile));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			parser = new attributeParser(tokens);
			parser.spec_list();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (RecognitionException e) {
			e.printStackTrace();
			return;
		}
		AttributeTable attTable = parser.getAttributeTable();
		try {
			BufferedWriter bufMod = new BufferedWriter(new FileWriter(Strings
					.getString("module-dir")
					+ Strings.getString("Attributes.module-name")
					+ Strings.getString("extension")));
			BufferedWriter bufInterf = new BufferedWriter(new FileWriter(
					Strings.getString("interface-dir")
							+ Strings.getString("Attributes.interface-set")
							+ Strings.getString("extension")));
			BufferedWriter bufConfig = new BufferedWriter(new FileWriter(
					Strings.getString("module-dir")
							+ Strings.getString("Attributes.config-name")
							+ Strings.getString("extension")));

			bufMod.append(Strings.getString("file-header") + "\n");
			bufInterf.append(Strings.getString("file-header") + "\n");
			bufConfig.append(Strings.getString("file-header") + "\n");

			attTable.writeToBuffer(bufMod, bufInterf, bufConfig);
			bufMod.close();
			bufInterf.close();
			bufConfig.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
