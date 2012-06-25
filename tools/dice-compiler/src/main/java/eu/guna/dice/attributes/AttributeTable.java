/**
 * 
 */
package eu.guna.dice.attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import eu.guna.dice.attributes.exceptions.AttributeAlreadyDefinedException;
import eu.guna.dice.common.Strings;

/**
 * An attribute lookup table used during the compilation process.
 * 
 * @see net.dice.attributes.AttributesCompiler
 * @see net.dice.attributes.Attribute
 * @author Stefan Guna
 * 
 */
class AttributeTable {
	private static final String KW_ID = "id";
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

	private void printConfiguration(BufferedWriter buffer, String timerDecls,
			String timerLinks) throws IOException {
		buffer.append("#include \"Timer.h\"\n\n");
		
		buffer.append("configuration "
				+ Strings.getString("Attributes.config-name") + " {\n");
		buffer.append("  provides interface "
				+ Strings.getString("Attributes.interface-read") + ";\n");
        buffer.append("  provides interface DiceReport;\n");
		buffer.append("  uses interface "
				+ Strings.getString("Attributes.interface-set") + ";\n");
		buffer.append("}\n\n");

		buffer.append("implementation {\n");
		buffer.append("  components MainC;\n");
		buffer.append("  components LedsC;\n");
		buffer.append("  components HilTimerMilliC as Time;\n");
		buffer.append("  components "
				+ Strings.getString("Attributes.module-name") + ";\n");
		buffer.append("  components DeltaSpeedC;\n");
		buffer.append(timerDecls);
		buffer.append("\n");

		buffer.append("  " + Strings.getString("Attributes.module-name")
				+ ".Boot -> MainC;\n\n");
		buffer.append("  DeltaSpeedC.ReadRawAttributes -> "
				+ Strings.getString("Attributes.module-name")
				+ ".ReadRawAttributes;\n");

		buffer.append("  " + Strings.getString("Attributes.module-name")
				+ ".Leds -> LedsC;\n");

		buffer.append("  " + Strings.getString("Attributes.module-name") + "."
				+ Strings.getString("Attributes.interface-set") + " = "
				+ Strings.getString("Attributes.interface-set") + ";\n");
		buffer.append("  " + Strings.getString("Attributes.interface-read")
				+ " = DeltaSpeedC."
				+ Strings.getString("Attributes.interface-read") + ";\n");
        buffer.append("  DiceReport = DeltaSpeedC.DiceReport;\n\n");
		buffer.append(timerLinks);
		buffer.append("}\n");
	}

	private void printInterfaceFooter(BufferedWriter buffer) throws IOException {
		buffer.append("}\n");
	}

	private void printInterfaceHeader(BufferedWriter buffer) throws IOException {
		buffer.append("interface "
				+ Strings.getString("Attributes.interface-set") + " {\n");
		buffer.append("  event void set_attribute(int hash, void * value);\n");
	}

	private void printModuleFooter(BufferedWriter buffer) throws IOException {
		buffer.append("}\n");
	}

	private void printModuleHeader(BufferedWriter buffer,
			StringBuffer bufUsesTimers) throws IOException {
		buffer.append("module " + Strings.getString("Attributes.module-name")
				+ " {\n");
		buffer.append("  provides interface "
				+ Strings.getString("Attributes.interface-read")
				+ " as ReadRawAttributes;\n");
		buffer.append("  uses interface "
				+ Strings.getString("Attributes.interface-set") + ";\n");
		buffer.append("  uses interface Boot;\n");
		buffer.append("  uses interface Leds;\n");
		buffer.append(bufUsesTimers.toString());
		buffer.append("}\n\n");
		buffer.append("implementation {\n");
		buffer.append("  uint16_t id;\n");
	}

	private void writeData(StringBuffer bufDecl, StringBuffer bufGetValue,
			StringBuffer bufPersist, StringBuffer bufSetValue,
			StringBuffer bufInterface, StringBuffer bufBooted,
			StringBuffer bufModFunctions, StringBuffer bufUsesTimers,
			StringBuffer bufTimerComponents, StringBuffer bufTimerLinks) {

		for (Iterator<Attribute> it = data.values().iterator(); it.hasNext();) {
			Attribute att = it.next();

			if (att.getValue().getType() == Value.Type.STATIC)
				bufInterface.append("  command " + att.getType()
						+ " get_" + att.getName() + "();\n"); //$NON-NLS-2$

			if (att.getValue().getType() == Value.Type.DYNAMIC) {
				bufInterface.append("  command " + att.getType()
						+ " get_" + att.getName() + "(bool *dontWait);\n"); //$NON-NLS-2$

				bufInterface.append("  event void set_" //$NON-NLS-2$
						+ att.getName() + "(" + att.getType() + " val);\n");

				bufModFunctions
						.append("\n  event void " + Strings.getString("Attributes.interface-set") //$NON-NLS-2$
								+ ".set_" + att.getName() + "(" + att.getType() //$NON-NLS-2$
								+ " val)\n  {\n");
				bufModFunctions
						.append("    new_" + att.getName() + " = val;\n"); //$NON-NLS-2$
				bufModFunctions
						.append("    signal ReadRawAttributes.attributeChanged("
								+ (att.getName().hashCode() & 0xFFFF)
								+ "U, (void *) &new_" + att.getName() + ");\n");
				bufModFunctions.append("  }\n");

				bufUsesTimers.append("  uses interface Timer<TMilli> as Timer"
						+ att.getName() + ";\n");

				bufBooted.append("    call Timer" + att.getName()
						+ ".startPeriodic(" + att.getValue().getPeriod()
						+ ");\n");

				bufTimerComponents
						.append("  components new TimerMilliC() as Timer"
								+ att.getName() + ";\n");
				bufTimerLinks.append("  "
						+ Strings.getString("Attributes.module-name")
						+ ".Timer" + att.getName() + " -> Timer"
						+ att.getName() + ";\n");

				bufModFunctions.append("\n  event void Timer" + att.getName()
						+ ".fired()\n  {\n");
				bufModFunctions.append("    bool dontWait = FALSE;\n");
				bufModFunctions
						.append("    "
								+ att.getType()
								+ " temp_val = call " + Strings.getString("Attributes.interface-set") + ".get_" //$NON-NLS-2$ //$NON-NLS-3$
								+ att.getName() + "(&dontWait);\n");
				bufModFunctions.append("    if (dontWait == TRUE) {\n");
				bufModFunctions
						.append("      new_" + att.getName() + " = temp_val;\n"); //$NON-NLS-2$
				bufModFunctions
						.append("      signal ReadRawAttributes.attributeChanged("
								+ (att.getName().hashCode() & 0xFFFF)
								+ "U, (void *) &new_" + att.getName() + ");\n");
				bufModFunctions.append("    }\n");
				bufModFunctions.append("  }\n");
				bufPersist.append("      case "
						+ (att.getName().hashCode() & 0xFFFF) + "U:\n");
				bufPersist.append("        " + att.getName() + " = new_"
						+ att.getName() + ";\n");
				bufPersist.append("        break;\n");
			}

			if (att.getValue().getType() == Value.Type.STATIC)
				bufBooted
						.append("    new_" + att.getName() + " = call " //$NON-NLS-2$
								+ Strings.getString("Attributes.interface-set") + ".get_" + att.getName() //$NON-NLS-2$
								+ "();\n");

			bufDecl.append("  " + att.toString() + "\n"); //$NON-NLS-2$

			bufGetValue
					.append("      case " + (att.getName().hashCode() & 0xFFFF) + "U:\n"); //$NON-NLS-2$
			bufGetValue.append("        return (void *) &" + att.getName()
					+ ";\n");
			bufSetValue
					.append("      case " + (att.getName().hashCode() & 0xFFFF) + "U:\n"); //$NON-NLS-2$
			if (att.getValue().getType() == Value.Type.DYNAMIC) {
				bufSetValue.append("        new_" + att.getName() + " = *(("
						+ att.getType() + " *) value);\n");
				bufSetValue
						.append("        signal ReadRawAttributes.attributeChanged("
								+ (att.getName().hashCode() & 0xFFFF)
								+ "U, (void *) &new_" + att.getName() + ");\n");
			} else {
				bufSetValue.append("        " + att.getName() + " = *(("
						+ att.getType() + " *) value);\n");
				bufSetValue
						.append("        signal ReadRawAttributes.attributeChanged("
								+ (att.getName().hashCode() & 0xFFFF)
								+ "U, (void *) &" + att.getName() + ");\n");
			}
			bufSetValue.append("        break;\n");
		}
	}

	/**
	 * Writes the attribute table to a buffer.
	 * 
	 * @param bufMod
	 *            The buffer to write code to.
	 * @param bufInterf
	 *            The buffer to write the interface to.
	 * @param bufConfig
	 *            The buffer to write the configuration to.
	 * @throws IOException
	 *             When an error ocurred while writing to the buffer.
	 */
	protected void writeToBuffer(BufferedWriter bufMod,
			BufferedWriter bufInterf, BufferedWriter bufConfig)
			throws IOException {
		StringBuffer bufDecl = new StringBuffer();
		StringBuffer bufGetValue = new StringBuffer();
		StringBuffer bufSetValue = new StringBuffer();
		StringBuffer bufBooted = new StringBuffer();
		StringBuffer bufInterface = new StringBuffer();
		StringBuffer bufModFunctions = new StringBuffer();
		StringBuffer bufUsesTimers = new StringBuffer();
		StringBuffer bufTimerComponents = new StringBuffer();
		StringBuffer bufTimerLinks = new StringBuffer();
		StringBuffer bufPersist = new StringBuffer();

		bufGetValue
				.append("\n  command void * ReadRawAttributes.attributeValue(uint16_t hash)\n  {\n");
		bufGetValue.append("    switch (hash) {\n");
		bufGetValue
				.append("      case " + (KW_ID.hashCode() & 0xFFFF) + "U:\n");
		bufGetValue.append("        return (void *) &id;\n");

		bufPersist
				.append("\n  command void ReadRawAttributes.persistAttribute(uint16_t hash)\n  {\n");
		bufPersist.append("    switch (hash) {\n");

		bufSetValue
				.append("\n  event void " + Strings.getString("Attributes.interface-set") //$NON-NLS-2$
						+ ".set_attribute(int hash, void *value)\n  {\n");
		bufSetValue.append("    switch (hash) {\n");

		writeData(bufDecl, bufGetValue, bufPersist, bufSetValue, bufInterface,
				bufBooted, bufModFunctions, bufUsesTimers, bufTimerComponents,
				bufTimerLinks);

		bufPersist.append("    }\n");
		bufPersist.append("  }\n");

		bufGetValue.append("    }\n");
		bufGetValue.append("    return NULL;\n  }\n");

		bufSetValue.append("    }\n");
		bufSetValue.append("  }\n");

		printModuleHeader(bufMod, bufUsesTimers);
		bufMod.append(bufDecl.toString());
		bufMod.append("\n  event void Boot.booted()\n  {\n");
		bufMod.append("    id = TOS_NODE_ID;\n");
		bufMod.append(bufBooted.toString());
		bufMod.append("  }\n");
		bufMod.append(bufGetValue.toString());
		bufMod.append(bufPersist.toString());
		bufMod
				.append("\n  command bool ReadRawAttributes.isKeyword(uint16_t hash)\n");
		bufMod.append("  {\n");
		bufMod.append("    switch (hash) {\n");
		bufMod.append("      case " + (KW_ID.hashCode() & 0xFFFF) + "U:\n");
		bufMod.append("        return TRUE;\n");
		bufMod.append("    }\n");
		bufMod.append("    return FALSE;\n");
		bufMod.append("  }\n");
		bufMod.append(bufSetValue.toString());
		bufMod.append(bufModFunctions.toString());
		printModuleFooter(bufMod);

		printInterfaceHeader(bufInterf);
		bufInterf.append(bufInterface.toString());
		printInterfaceFooter(bufInterf);

		printConfiguration(bufConfig, bufTimerComponents.toString(),
				bufTimerLinks.toString());
	}
}
