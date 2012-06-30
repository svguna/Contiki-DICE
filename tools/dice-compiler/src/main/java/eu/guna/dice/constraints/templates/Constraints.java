/**
 * 
 */
package eu.guna.dice.constraints.templates;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
			buf.append("        " + q.getType().toNesc()
					+ (it.hasNext() ? "," : "") + "\n");
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

		int i = 0;
		for (Iterator<Pattern> it = patterns.iterator(); it.hasNext();) {
			Pattern pattern = it.next();
			if (pattern.isDummy())
				continue;
			Set<Quantifier> quantifiers = pattern.getQuantifiers();

			buf.append("    .entries = {\n");

			buf.append("        { .attr = "
					+ (pattern.getAttribute().getHash() & 0xFFFF) + ",\n");

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
		int nodes_no = constraint.toContiki(buf);
		buf.append("    },\n");
		buf.append("    .nodes_no = " + nodes_no + ",\n");
		buf.append("};\n\n");

		return buf.toString();
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
							+ pi.getAttribute().getHash() + ",\n");
					buf.append("          .math_id = " + mId1 + ",\n");
					buf.append("          .quantifier = " + q.getId() + ",\n");
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
									+ pi.getAttribute().getHash() + ",\n");
							buf.append("          .math_id = " + mId1 + ",\n");
							buf.append("          .quantifier = " + q.getId()
									+ ",\n");
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
			throws QuantifierNotFoundException {

		BoolNode constraint = constraintTable.getConstraints().get(0);
		constraint.getQuantifiers();
		String funcConstraint = getConstraint(constraint, 0);
		String funcPattern = getAggregationPattern(constraint, 0);
		String funcMapping = getMappings(constraint, 0);
		if (funcConstraint != null && funcPattern != null) {
			System.out.println(funcConstraint);
			System.out.println(funcPattern);
			System.out.println(funcMapping);
		}
	}

	/**
	 * Dumps the constraints into the module that persists them.
	 * 
	 * @param constraintTable
	 *            The constraints list.
	 * @throws IOException
	 *             In case of error.
	 * @throws QuantifierNotFoundException
	 */
	public static void writeCode(ConstraintTable constraintTable)
			throws IOException, QuantifierNotFoundException {
		String outFilename = Strings.getString("module-dir")
				+ Strings.getString("Constraints.constraint-output-file");
		BufferedWriter out = new BufferedWriter(new FileWriter(outFilename));
		log.info("Generating " + outFilename);

		out.write(Strings.getString("file-header") + "\n");

		out.write(getHeader());

		BoolNode constraint = constraintTable.getConstraints().get(0);
		constraint.getQuantifiers();
		String funcConstraint = getConstraint(constraint, 0);
		String funcPattern = getAggregationPattern(constraint, 0);
		String funcMapping = getMappings(constraint, 0);
		if (funcConstraint != null && funcPattern != null) {
			out.write(funcConstraint);
			out.write(funcPattern);
			out.write(funcMapping);
		}

		out.close();
	}
}
