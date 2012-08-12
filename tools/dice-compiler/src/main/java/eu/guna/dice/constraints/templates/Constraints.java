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
package eu.guna.dice.constraints.templates;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.guna.dice.common.Strings;
import eu.guna.dice.constraints.BoolNode;
import eu.guna.dice.constraints.ConstraintTable;
import eu.guna.dice.constraints.Pattern;
import eu.guna.dice.constraints.Quantifier;
import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;

/**
 * Templates for writing the constraints attribution code. Builds the file that
 * handle constraint persistence.
 * 
 * @author Stefan Guna
 * 
 */
public class Constraints {

	private static Logger log = Logger.getLogger(Constraints.class);

	private static void appendQuantifiers(BoolNode constraint, StringBuffer buf)
			throws QuantifierNotFoundException {
		List<Quantifier> quantifiers = constraint.getQuantifiers();
		int i = 0;

		buf.append("    .quantifiers = {\n");
		for (Iterator<Quantifier> it = quantifiers.iterator(); it.hasNext();) {
			Quantifier q = it.next();
			buf.append("        " + q.getType().toNesc() + ", /* "
					+ q.getName() + " */ \n");
			i++;
		}
		buf.append("    },\n");
		buf.append("    .quantifiers_no = " + i + ",\n");
	}

	/**
	 * Builds the string representations of functions that set aggregation
	 * patterns.
	 * 
	 * @param constraint
	 *            The constraint from which to get the aggregation patterns.
	 * @param id
	 *            The ID of the functions.
	 * @return a string representing the functions code that set the aggregation
	 *         patterns for the given constraint
	 * @throws QuantifierNotFoundException
	 */
	private static String getAggregationPattern(BoolNode constraint, int id)
			throws QuantifierNotFoundException {
		StringBuffer buf = new StringBuffer();
		List<Pattern> patterns = constraint.getPatterns();
		if (patterns == null || patterns.size() == 0)
			return null;

		buf.append("signature_t signature = {\n");
		buf.append("    .entries = {\n");

		int i = 0;
		for (Iterator<Pattern> it = patterns.iterator(); it.hasNext();) {
			Pattern pattern = it.next();
			if (pattern.isDummy())
				continue;
			Set<Quantifier> quantifiers = pattern.getQuantifiers();

			buf.append("        { .attr = "
					+ (pattern.getAttribute().getHash() & 0xFFFF) + ", /* "
					+ pattern.getAttribute().getName() + " */\n");

			buf.append("          .objective = " + pattern.getObjective()
					+ ",\n");
			buf.append("          .slice_size = " + quantifiers.size() + ",\n");
			buf.append("        },\n");
			i++;
		}

		buf.append("    },\n");
		buf.append("    .entries_no = " + i + ",\n");
		buf.append("};\n\n");
		return buf.toString();
	}

	private static String getConstraint(BoolNode constraint, int id)
			throws QuantifierNotFoundException {
		StringBuffer buf = new StringBuffer();
		if (constraint.getType() == BoolNode.Type.LEAF)
			return null;

		buf.append("invariant_t invariant = {\n");
		appendQuantifiers(constraint, buf);
		buf.append("    .nodes = {\n");
		int nodes_no = constraint.toContiki(false, buf);
		buf.append("    },\n");
		buf.append("    .nodes_no = " + nodes_no + ",\n");
		buf.append("};\n\n");

		buf.append("invariant_t disjunctions[0];\n\n");
		buf.append("int disjunctions_no = 0;\n\n");

		return buf.toString();
	}

	private static String getEmptyAggregationPattern(int i) {
		return "signature_t signature;\n\n";
	}

	private static String getEmptyMappings(int i) {
		return "mapping_t mapping;\n\n";
	}

	private static String getHeader() {
		StringBuffer buf = new StringBuffer();

		buf.append("#include \"attributes.h\"\n");
		buf.append("#include \"invariant.h\"\n");

		buf.append("\n");
		return buf.toString();
	}

	private static String getMappings(BoolNode constraint, int id)
			throws QuantifierNotFoundException {
		StringBuffer buf = new StringBuffer();
		ArrayList<Pattern> patterns = constraint.getPatterns();
		int startOffset = 0;
		HashSet<Integer> done = new HashSet<Integer>();
		Pattern pi;
		int index = 0;

		buf.append("mapping_t mapping = {\n");
		buf.append("    .data = {\n");
		for (int i = 0; i < patterns.size(); i++, startOffset += pi
				.getQuantifiers().size()) {

			pi = patterns.get(i);
			log.debug(pi);
			HashMap<Integer, ArrayList<Quantifier>> mappingsi = pi
					.getMappings();

			for (Iterator<Integer> it1 = mappingsi.keySet().iterator(); it1
					.hasNext();) {

				Integer mId1 = it1.next();

				if (done.contains(mId1))
					continue;
				done.add(mId1);

				log.debug(mId1 + " " + mappingsi.get(mId1) + "off:"
						+ startOffset);

				int k = startOffset;
				for (Quantifier q : mappingsi.get(mId1)) {
					buf.append("        { .attribute = "
							+ pi.getAttribute().getHash() + ", /* "
							+ pi.getAttribute().getName() + " */\n");
					buf.append("          .math_id = " + mId1 + ",\n");
					buf.append("          .quantifier = " + q.getId() + ", /* "
							+ q.getName() + " */\n");
					buf.append("          .index = " + k + "\n");
					buf.append("        },\n");
					k++;
					index++;
				}

				int startOffset2 = startOffset + pi.getQuantifiers().size();
				Pattern pj;
				for (int j = i + 1; j < patterns.size(); j++, startOffset2 += pj
						.getQuantifiers().size()) {
					pj = patterns.get(j);
					HashMap<Integer, ArrayList<Quantifier>> mappingsj = pj
							.getMappings();
					for (Iterator<Integer> it2 = mappingsj.keySet().iterator(); it2
							.hasNext();) {

						Integer mId2 = it2.next();
						if (mId1 != mId2)
							continue;

						log.debug(mId1 + " " + mappingsj.get(mId1)
								+ startOffset2);
						k = startOffset2;
						for (Quantifier q : mappingsj.get(mId1)) {
							buf.append("        { .attribute = "
									+ pi.getAttribute().getHash() + ", /* "
									+ pi.getAttribute().getName() + " */\n");
							buf.append("          .math_id = " + mId1 + ",\n");
							buf.append("          .quantifier = " + q.getId()
									+ ", /*" + q.getName() + " */\n");
							buf.append("          .index = " + k + "\n");
							buf.append("        },\n");
							k++;
							index++;
						}
					}
				}
			}
		}
		buf.append("    },\n");
		buf.append("    .data_no = " + index + ",\n");
		buf.append("};\n\n");
		return buf.toString();
	}

	private static String getType1Constraint(BoolNode constraint, int id)
			throws QuantifierNotFoundException {
		StringBuffer buf = new StringBuffer();
		if (constraint.getType() == BoolNode.Type.LEAF)
			return null;

		List<Quantifier> quantifiers = constraint.getQuantifiers();

		Set<BoolNode> disjunctions = constraint.extractDisjunctions();
		log.info(quantifiers);

		buf.append("invariant_t disjunctions[] = {\n");

		for (BoolNode disjunction : disjunctions) {
			buf.append("  {\n");

			int i = 0;

			buf.append("    .quantifiers = {\n");
			for (Iterator<Quantifier> it = quantifiers.iterator(); it.hasNext();) {
				Quantifier q = it.next();
				buf.append("        " + q.getType().toNesc() + ", /* "
						+ q.getName() + " */\n");
				i++;
			}
			buf.append("    },\n");
			buf.append("    .quantifiers_no = " + i + ",\n");

			buf.append("    .nodes = {\n");
			int nodes_no = disjunction.toContiki(true, buf);
			buf.append("    },\n");
			buf.append("    .nodes_no = " + nodes_no + ",\n");
			buf.append("  },\n\n");
		}

		buf.append("};\n\n");
		buf.append("int disjunctions_no = " + disjunctions.size() + ";\n");
		buf.append("invariant_t invariant;\n\n");
		return buf.toString();
	}

	/**
	 * Dumps the constraints on the stdout.
	 * 
	 * @param constraintTable
	 *            The constraints list.
	 * @throws IOException
	 *             In case of error.
	 * @throws QuantifierNotFoundException
	 */
	public static void printCode(ConstraintTable constraintTable)
			throws QuantifierNotFoundException, IOException {
		writeToBuffer(constraintTable, System.out);
	}

	/**
	 * Dumps the constraints into the module that persists them.
	 * 
	 * @param constraintTable
	 *            The constraints list.
	 * @param outputDirectory
	 * @throws IOException
	 *             In case of error.
	 * @throws QuantifierNotFoundException
	 */
	public static void writeCode(ConstraintTable constraintTable,
			String outputDirectory) throws IOException,
			QuantifierNotFoundException {
		String outFilename = outputDirectory
				+ System.getProperty("file.separator")
				+ Strings.getString("Constraints.constraint-output-file");
		PrintStream out = new PrintStream(new File(outFilename));

		log.info("Generating " + outFilename);

		writeToBuffer(constraintTable, out);

		out.close();
	}

	private static void writeToBuffer(ConstraintTable constraintTable,
			PrintStream out) throws IOException, QuantifierNotFoundException {
		out.println(Strings.getString("file-header"));
		out.print(getHeader());

		BoolNode constraint = constraintTable.getConstraints().get(0);
		boolean isType2 = constraint.isType2();
		log.debug("Is type 2: " + isType2);
		constraint.getQuantifiers();
		String funcConstraint = null, funcPattern = null, funcMapping = null;

		if (isType2) {
			funcConstraint = getConstraint(constraint, 0);
			funcPattern = getAggregationPattern(constraint, 0);
			funcMapping = getMappings(constraint, 0);
		} else {
			funcConstraint = getType1Constraint(constraint, 0);
			funcPattern = getEmptyAggregationPattern(0);
			funcMapping = getEmptyMappings(0);
		}
		out.print(funcConstraint);
		out.print(funcPattern);
		out.print(funcMapping);
	}
}
