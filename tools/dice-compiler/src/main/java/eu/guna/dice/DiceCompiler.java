package eu.guna.dice;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import eu.guna.dice.attributes.AttributesCompiler;
import eu.guna.dice.constraints.ConstraintsCompiler;

/**
 * @author Stefan Guna
 * 
 *         Top level compiler
 * 
 */
public class DiceCompiler {
	@Option(name = "-a", usage = "compile attributes")
	private boolean parseAttributes;

	@Option(name = "-c", usage = "compile constraints")
	private boolean parseConstraints;

	@Option(name = "-p", usage = "just print the code for constraints")
	private boolean justPrint;

	@Argument
	private String fileName;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.OFF);

		DiceCompiler diceCompiler = new DiceCompiler();
		CmdLineParser parser = new CmdLineParser(diceCompiler);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			return;
		}

		if (!diceCompiler.parseAttributes && !diceCompiler.parseConstraints) {
			diceCompiler.parseAttributes = true;
			diceCompiler.parseConstraints = true;
		}

		if (diceCompiler.justPrint)
			diceCompiler.parseAttributes = false;

		if (diceCompiler.parseAttributes)
			AttributesCompiler.parse(diceCompiler.fileName + ".att");

		if (diceCompiler.parseConstraints)
			ConstraintsCompiler.parse(diceCompiler.fileName + ".dc",
					diceCompiler.justPrint);
	}
}
