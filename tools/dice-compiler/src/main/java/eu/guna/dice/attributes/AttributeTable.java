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
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import eu.guna.dice.attributes.exceptions.AttributeAlreadyDefinedException;

/**
 * An attribute lookup table used during the compilation process.
 * 
 * @see net.dice.attributes.AttributesCompiler
 * @see net.dice.attributes.Attribute
 * @author Stefan Guna
 * 
 */
class AttributeTable {
	/** Internal data */
	private HashMap<String, Attribute> data;

	/** The constructor. */
	protected AttributeTable() {
		data = new HashMap<String, Attribute>();
	}

	/**
	 * Adds an attribute to the table.
	 * 
	 * @throws AttributeAlreadyDefinedException
	 *             When trying to add an attribute that already exists.
	 */
	protected void addAttribute(Attribute att)
			throws AttributeAlreadyDefinedException {
		if (data.get(att.getName()) != null)
			throw new AttributeAlreadyDefinedException(att.getName());
		data.put(att.getName(), att);
	}

	private void printModuleFooter(BufferedWriter buffer) throws IOException {
		buffer.append("}\n\n");
	}

	private void printModuleHeader(BufferedWriter buffer) throws IOException {

		buffer.append("static void generic_update(uint16_t attr, int new_val)\n");
		buffer.append("{\n");
		buffer.append("    int updated = 0;\n");
		buffer.append("    view_entry_t entry;\n\n");
		buffer.append("    entry.ts = clock_time();\n");
		buffer.append("    entry.val = new_val;\n");
		buffer.append("    entry.attr = attr;\n");
		buffer.append("    memcpy(&entry.src, &rimeaddr_node_addr, sizeof(rimeaddr_t));\n\n");

		buffer.append("    if (local_disjunctions_refresh()) {\n");
		buffer.append("        updated = 1;\n");
		buffer.append("        print_viewt1_msg(\"T1 ar\", &local_view_t1);\n");
		buffer.append("    }\n");
		buffer.append("    print_entry_msg(\"attribute refresh \", &entry);\n");
		buffer.append("    if (push_entry(&entry)) {\n");
		buffer.append("        print_view_msg(\"after refresh \", &local_view);\n");
		buffer.append("        updated = 1;\n");
		buffer.append("    }\n");
		buffer.append("    if (updated)\n");
		buffer.append("        drickle_reset();\n");
		buffer.append("}\n\n");

	}

	private void writeData(StringBuffer bufDecl, StringBuffer bufGetValue,
			StringBuffer bufHeader, StringBuffer bufInit,
			StringBuffer bufSetFunctions) {

		bufDecl.append("int local_attribute_hashes[] = {\n");
		for (Attribute att : data.values())
			bufDecl.append("    " + (att.getName().hashCode() & 0xFFFF)
					+ ", \n");
		bufDecl.append("};\n");
		bufDecl.append("int local_attribute_no = " + data.values().size()
				+ ";\n");

		for (Iterator<Attribute> it = data.values().iterator(); it.hasNext();) {
			Attribute att = it.next();

			if (att.getValue().getType() == Value.Type.DYNAMIC) {
				bufHeader.append(att.getType() + " get_" + att.getName()
						+ "(); /* TO BE IMPLEMENTED */\n");

				bufSetFunctions.append("static void set_" + att.getName()
						+ "(void *data)\n");
				bufSetFunctions.append("{\n");
				bufSetFunctions.append("    " + att.getType()
						+ " new_val = get_" + att.getName() + "();\n\n");

				bufSetFunctions.append("    if (new_val != attribute_"
						+ att.getName() + ")\n");
				bufSetFunctions
						.append("        generic_update("
								+ (att.getName().hashCode() & 0xFFFF)
								+ ", new_val);\n");
				bufSetFunctions.append("    ctimer_reset(&att_" + att.getName()
						+ "_timer);\n");
				bufSetFunctions.append("}\n\n");

				bufInit.append("    period = " + att.getValue().getPeriod()
						+ " * CLOCK_SECOND;\n");
				bufInit.append("    ctimer_set(&att_" + att.getName()
						+ "_timer, period, &set_" + att.getName()
						+ ", NULL);\n");

				bufDecl.append("static struct ctimer att_" + att.getName()
						+ "_timer;\n");
			}

			if (att.getValue().getType() == Value.Type.EVENT) {
				bufHeader.append("void set_" + att.getName() + "("
						+ att.getType() + " new_value); /* TO CALL */\n");

				bufSetFunctions.append("void set_" + att.getName() + "("
						+ att.getType() + " new_value)\n");
				bufSetFunctions.append("{\n");
				bufSetFunctions.append("    " + att.getType()
						+ " new_val = new_value;\n\n");

				bufSetFunctions.append("    if (new_val != attribute_"
						+ att.getName() + ")\n");
				bufSetFunctions
						.append("        generic_update("
								+ (att.getName().hashCode() & 0xFFFF)
								+ ", new_val);\n");
				bufSetFunctions.append("}\n\n");
			}

			bufGetValue.append("      case "
					+ (att.getName().hashCode() & 0xFFFF) + ": /* "
					+ att.getName() + " */\n");
			if (att.getValue().getType() == Value.Type.CONSTANT)
				bufGetValue.append("        *value = "
						+ att.getValue().getValue() + ";\n");
			else {
				bufDecl.append(att.getType() + " attribute_" + att.getName()
						+ ";\n");
				bufGetValue.append("        *value = attribute_"
						+ att.getName() + ";\n");
			}
			bufGetValue.append("        return 1;\n");
		}
	}

	/**
	 * Writes the attribute table to a buffer.
	 * 
	 * @param bufCode
	 *            The buffer to write code to.
	 * @param bufHeader
	 *            TODO
	 * @throws IOException
	 *             When an error ocurred while writing to the buffer.
	 */
	protected void writeToBuffer(BufferedWriter bufCode,
			BufferedWriter bufHeader) throws IOException {
		StringBuffer bufDecl = new StringBuffer();
		StringBuffer bufGetValue = new StringBuffer();
		StringBuffer bufInit = new StringBuffer();
		StringBuffer bufSetFunctions = new StringBuffer();
		StringBuffer bufHeaderFile = new StringBuffer();

		bufGetValue
				.append("int get_attribute(uint16_t hash, uint16_t *value)\n");
		bufGetValue.append("{\n");
		bufGetValue.append("    switch (hash) {\n");
		bufGetValue.append("      case " + ("id".hashCode() & 0xFFFF)
				+ ": /* id */\n");
		bufGetValue
				.append("        *value = (rimeaddr_node_addr.u8[1] << 8) + (rimeaddr_node_addr.u8[0]);\n");
		bufGetValue.append("        return 1;\n");

		bufInit.append("void attributes_init()\n");
		bufInit.append("{\n");
		bufInit.append("    clock_time_t period;\n");

		writeData(bufDecl, bufGetValue, bufHeaderFile, bufInit, bufSetFunctions);

		bufGetValue.append("    }\n");
		bufGetValue.append("    return 0;\n");
		bufGetValue.append("}\n\n");

		bufCode.append("#include \"rime.h\"\n");
		bufCode.append("#include \"attributes.h\"\n");
		bufCode.append("#include \"dice.h\"\n");
		bufCode.append("#include \"view_manager.h\"\n");
		bufCode.append("#include \"drickle.h\"\n\n");

		bufCode.append(bufDecl);
		bufCode.append("\n");
		printModuleHeader(bufCode);
		bufCode.append(bufSetFunctions);
		bufCode.append(bufGetValue);
		bufCode.append(bufInit);
		printModuleFooter(bufCode);

		bufHeader.append("#ifndef __ATTRIBUTES_H\n");
		bufHeader.append("#define __ATTRIBUTES_H\n\n");
		bufHeader.append("#include \"rime.h\"\n\n");
		bufHeader.append(bufHeaderFile);

		bufHeader.append("\n");
		bufHeader.append("void attributes_init();\n\n");
		bufHeader.append("extern int local_attribute_hashes["
				+ data.values().size() + "];\n");
		bufHeader.append("extern int local_attribute_no;\n");

		bufHeader.append("#endif\n");
	}
}
