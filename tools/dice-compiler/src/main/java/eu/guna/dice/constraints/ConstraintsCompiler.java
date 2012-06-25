/**
 * 
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

		parse(Strings.getString("Constraints.constraint-file"), justPrint);

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
