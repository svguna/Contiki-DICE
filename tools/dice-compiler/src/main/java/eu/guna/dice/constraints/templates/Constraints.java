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
import eu.guna.dice.constraints.Pattern.Objective;
import eu.guna.dice.constraints.Quantifier;
import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;

/**
 * Templates for writing the constraints attribution code. Builds the nesC
 * module that handle constraint persistence.
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
		for (Iterator<Quantifier> it = quantifiers.iterator(); it.hasNext();) {
			Quantifier q = it.next();
			buf.append("      constraint->quantifiers[" + q.getId() + "] = "
					+ q.getType().toNesc() + ";\n");
			i++;
		}
		buf.append("      constraint->quantifiers_no = " + i + ";\n");
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

		buf.append("  void fillPattern" + id + "()\n" + "  {\n");
		buf.append("     pattern_t *pattern = patterns + " + id + ";\n");
		buf.append("     pattern->in_use = TRUE;\n");

		int i = 0;
		for (Iterator<Pattern> it = patterns.iterator(); it.hasNext();) {
			Pattern pattern = it.next();
			if (pattern.isDummy())
				continue;
			Set<Quantifier> quantifiers = pattern.getQuantifiers();

			buf.append("     pattern->fields[" + i + "].hash = "
					+ (pattern.getAttribute().getHash() & 0xFFFF) + "U;\n");

			buf.append("     pattern->fields[" + i + "].objective = "
					+ pattern.getObjective() + ";\n");
			if (!pattern.isQuantifier_bound()) {
				buf.append("     pattern->fields[" + i + "].data = "
						+ quantifiers.size() + ";\n");
				buf.append("     pattern->fields[" + i
						+ "].quantifier = INVALID_QUANTIFIER;\n");
			} else {
				buf.append("     pattern->fields[" + i + "].data = ");
				if (pattern.getObjective() == Objective.SCOPING)
					buf.append(pattern.getBoolNodeId());
				else
					buf.append(pattern.getImplicationId());
				buf.append(";\n");
				buf.append("     pattern->fields[" + i + "].quantifier = "
						+ quantifiers.iterator().next().getId() + ";\n");
			}

			int link = i++;
			for (Iterator<String> linkedIterator = pattern
					.getLinkedAttributes().iterator(); linkedIterator.hasNext();) {
				String attName = linkedIterator.next();
				buf.append("     pattern->fields[" + i + "].hash = "
						+ (attName.hashCode() & 0xFFFF) + "U;\n");
				buf.append("     pattern->fields[" + i
						+ "].objective = OBJ_LINK;\n");
				buf.append("    pattern->fields[" + i + "].data = " + link
						+ ";\n");
				buf.append("     pattern->fields[" + i
						+ "].quantifier = INVALID_QUANTIFIER;\n");
				i++;
			}
			continue;
		}

		buf.append("     pattern->fields_no = " + i + ";\n");
		buf.append("     signal PatternEngine.patternUpdated(pattern);\n");
		buf.append("  }\n\n");

		return buf.toString();
	}

	private static String getBooted(int count) {
		StringBuffer buf = new StringBuffer();
		buf.append("  event void Boot.booted()\n");
		buf.append("  {\n");
		buf.append("     int i;\n");
		buf.append("     tuple_t *tuple;\n");
		buf.append("     for (i = " + count + "; i < MAX_CONSTRAINTS; i++) {\n");
		buf.append("       constraints[i].in_use = FALSE;\n");
		buf.append("       patterns[i].in_use = FALSE;\n");
		buf.append("     }\n");
		for (int i = 0; i < count; i++) {
			buf.append("     fillConstraint" + i + "();\n");
			buf.append("     fillMapping" + i + "();\n");
			buf.append("     tuple = call TupleManager.getTuple(" + i + ");\n");
			buf.append("     tuple->constraint_id = " + i + ";\n");
			buf.append("     fillPattern" + i + "();\n");
		}
		buf.append("   }\n\n");
		return buf.toString();
	}

	private static String getConstraint(BoolNode constraint, int id)
			throws QuantifierNotFoundException {
		StringBuffer buf = new StringBuffer();
		if (constraint.getType() == BoolNode.Type.LEAF)
			return null;

		buf.append("  void fillConstraint" + id + "()\n" + "  {\n");
		buf.append("     constraint_t *constraint = constraints + " + id
				+ ";\n");
		buf.append("     constraint->in_use = TRUE;\n");
		buf.append("     constraint->ver = 1;\n");
		appendQuantifiers(constraint, buf);
		int nodes_no = constraint.toNesc(buf);
		buf.append("     constraint->nodes_no = " + nodes_no + ";\n");
		buf.append("  }\n\n");

		return buf.toString();
	}

	private static String getFooter() {
		StringBuffer buf = new StringBuffer();
		buf.append("  command constraint_t * ConstraintPersistence.getConstraint(int id)\n");
		buf.append("  {\n");
		buf.append("     if (id < 0 || id > MAX_CONSTRAINTS)\n");
		buf.append("       return NULL;\n");
		buf.append("     if (constraints[id].in_use == FALSE)\n");
		buf.append("       return NULL;\n");
		buf.append("     return constraints + id;\n");
		buf.append("  }\n\n");

		buf.append("  command void ConstraintPersistence.setConstraint(int id, constraint_t *constraint)\n");
		buf.append("  {\n");
		buf.append("     if (id < 0 || id > MAX_CONSTRAINTS)\n");
		buf.append("       return;\n");
		buf.append("     memcpy(constraints + id, constraint, sizeof(constraint_t));\n");
		buf.append("  }\n\n");

		buf.append("  command void PatternEngine.setPattern(int id, pattern_t *pattern)\n");
		buf.append("  {\n");
		buf.append("     if (id < 0 || id > MAX_CONSTRAINTS)\n");
		buf.append("       return;\n");
		buf.append("     memcpy(patterns + id, pattern, sizeof(pattern_t));\n");
		buf.append("     signal PatternEngine.patternUpdated(pattern);\n");
		buf.append("  }\n\n");

		buf.append("  command pattern_t * PatternEngine.getPattern(int id)\n");
		buf.append("  {\n");
		buf.append("     if (patterns[id].in_use)\n");
		buf.append("       return patterns + id;\n");
		buf.append("     return NULL;\n");
		buf.append("  }\n\n");

		buf.append("  command int PatternEngine.getIndex(uint8_t id, uint8_t math_id, \n"
				+ ""
				+ "                     uint16_t attribute, uint8_t quantifier)\n");
		buf.append("  {\n");
		buf.append("     mapping_t *mapping = mappings + id;\n");
		buf.append("     uint16_t i;\n");
		buf.append("     for (i = 0; i < mapping->size; i++)\n");
		buf.append("       if (mapping->data[i].math_id == math_id &&\n");
		buf.append("           mapping->data[i].attribute == attribute &&\n");
		buf.append("           mapping->data[i].quantifier == quantifier)\n");
		buf.append("         return mapping->data[i].index;\n");
		buf.append("     return -1;\n");
		buf.append("  }\n");

		buf.append("}\n");
		return buf.toString();
	}

	private static String getHeader() {
		StringBuffer buf = new StringBuffer();
		buf.append("#include \"constraint.h\"\n");
		buf.append("#include \"tuple.h\"\n");
		buf.append("module "
				+ Strings.getString("Constraints.constraint-module") + " {\n");
		buf.append("  uses interface Boot;\n");
		buf.append("  uses interface TupleManager;\n");
		buf.append("  provides interface ConstraintPersistence;\n");
		buf.append("  provides interface PatternEngine;\n");
		buf.append("}\n\n");

		buf.append("implementation {\n");
		buf.append("  constraint_t constraints[MAX_CONSTRAINTS];\n");
		buf.append("  pattern_t patterns[MAX_CONSTRAINTS];\n");

		buf.append("  mapping_t mappings[MAX_CONSTRAINTS];\n");

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

		buf.append("  void fillMapping" + id + "()\n" + "  {\n");
		buf.append("     mapping_t *mapping = mappings + " + id + ";\n");
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
					buf.append("     mapping->data[" + index + "]"
							+ ".math_id = " + mId1 + ";\n");
					buf.append("     mapping->data[" + index + "]"
							+ ".attribute = " + pi.getAttribute().getHash()
							+ ";\n");
					buf.append("     mapping->data[" + index + "]"
							+ ".quantifier = " + q.getId() + ";\n");
					buf.append("     mapping->data[" + index + "]"
							+ ".index = " + k + ";\n");
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
							buf.append("     mapping->data[" + index + "]"
									+ ".math_id = " + mId1 + ";\n");
							buf.append("     mapping->data[" + index + "]"
									+ ".attribute = "
									+ pi.getAttribute().getHash() + ";\n");
							buf.append("     mapping->data[" + index + "]"
									+ ".quantifier = " + q.getId() + ";\n");
							buf.append("     mapping->data[" + index + "]"
									+ ".index = " + k + ";\n");
							k++;
							index++;
						}
					}
				}
			}
		}
		buf.append("     mapping->size = " + index + ";\n");
		buf.append("  }\n");
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
		int constraintCount = 0;
		for (Iterator<BoolNode> it = constraintTable.getConstraints()
				.iterator(); it.hasNext();) {
			BoolNode constraint = it.next();
			constraint.getQuantifiers();
			String funcConstraint = getConstraint(constraint, constraintCount);
			String funcPattern = getAggregationPattern(constraint,
					constraintCount);
			String funcMapping = getMappings(constraint, constraintCount);
			if (funcConstraint != null && funcPattern != null) {
				System.out.println(funcConstraint);
				System.out.println(funcPattern);
				System.out.println(funcMapping);
				constraintCount++;
			}
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
		BufferedWriter out = new BufferedWriter(new FileWriter(
				Strings.getString("module-dir")
						+ Strings.getString("Constraints.constraint-module")
						+ Strings.getString("extension")));

		out.write(Strings.getString("file-header") + "\n");

		out.write(getHeader());

		int constraintCount = 0;
		for (Iterator<BoolNode> it = constraintTable.getConstraints()
				.iterator(); it.hasNext();) {
			BoolNode constraint = it.next();
			constraint.getQuantifiers();
			String funcConstraint = getConstraint(constraint, constraintCount);
			String funcPattern = getAggregationPattern(constraint,
					constraintCount);
			String funcMapping = getMappings(constraint, constraintCount);
			if (funcConstraint != null && funcPattern != null) {
				out.write(funcConstraint);
				out.write(funcPattern);
				out.write(funcMapping);
				constraintCount++;
			}
		}

		out.write(getBooted(constraintCount));
		out.write(getFooter());
		out.close();
	}
}
