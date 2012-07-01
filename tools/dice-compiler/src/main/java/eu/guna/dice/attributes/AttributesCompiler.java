/**
 * DICe - Distributed Invariants Checker
 * Monitors a global invariant like 
 * "forall m, n: temperature@m - temperature@n < T"
 * on a wireless sensor network.
 * Copyright (C) 2012 Stefan Guna, svguna@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
		parse(Strings.getString("Attributes.attribute-file"),
				Strings.getString("module-dir"));
	}

	/**
	 * Preprocess the attribute input file. The result is the TinyOS interfaces
	 * and modules that are linked with the application and the DiCE runtime.
	 * 
	 * @param inputFile
	 *            the attribute input file
	 */
	public static void parse(String inputFile, String outputDirectory) {
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

			String outName = outputDirectory
					+ Strings.getString("Attributes.attribute-output-file");
			String outHeaderName = outputDirectory
					+ Strings.getString("Attributes.attribute-output-header");

			BufferedWriter bufCode = new BufferedWriter(new FileWriter(outName));
			BufferedWriter bufHeader = new BufferedWriter(new FileWriter(
					outHeaderName));
			log.info("Outputting attributes to " + outName);

			bufCode.append(Strings.getString("file-header") + "\n\n");
			bufHeader.append(Strings.getString("file-header") + "\n\n");

			attTable.writeToBuffer(bufCode, bufHeader);
			bufCode.close();
			bufHeader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
