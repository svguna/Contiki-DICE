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
package eu.guna.dice.constraints;

import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import eu.guna.dice.common.LoggerConfiguration;
import eu.guna.dice.common.Strings;
import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;
import eu.guna.dice.constraints.templates.Constraints;

/**
 * @author Stefan Guna
 * 
 */
public class ConstraintsCompiler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LoggerConfiguration.setupLogging(Strings
				.getString("Constraints.logger-configuration"));
		boolean justPrint = args.length == 1 && args[0].equals("PRINT");

		parse(Strings.getString("Constraints.constraint-input-file"), justPrint);

	}

	public static void parse(String inputFile, boolean justPrint) {
		constraintParser parser;
		try {
			constraintLexer lexer = new constraintLexer(new ANTLRFileStream(
					inputFile));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			parser = new constraintParser(tokens);
			parser.spec_list();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (RecognitionException e) {
			e.printStackTrace();
			return;
		}
		try {
			if (justPrint)
				Constraints.printCode(parser.getConstraintTable());
			else
				Constraints.writeCode(parser.getConstraintTable());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (QuantifierNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public ConstraintsCompiler(String constraintText)
			throws RecognitionException, QuantifierNotFoundException {
		constraintParser parser;
		constraintLexer lexer = new constraintLexer(new ANTLRStringStream(
				constraintText));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		parser = new constraintParser(tokens);
		parser.spec_list();
	}
}
