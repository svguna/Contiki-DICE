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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a field of the aggregation pattern.
 * 
 * @author Stefan Guna
 * 
 */
public class Pattern {
	public enum Objective {
		MAXIMIZE {
			class ValueComparator implements Comparator<Quantifier> {
				Map<Quantifier, Integer> base;

				public ValueComparator(Map<Quantifier, Integer> base) {
					this.base = base;
				}

				public int compare(Quantifier a, Quantifier b) {
					if (base.get(b).equals(base.get(a)))
						return 1;
					return base.get(b).compareTo(base.get(a));
				}
			}

			protected Comparator<Quantifier> getComparator(
					Map<Quantifier, Integer> base) {
				return new ValueComparator(base);
			}

			public short toMessage() {
				return 0;
			}

			@Override
			public String toString() {
				return "OBJ_MAXIMIZE";
			}
		},
		MINIMIZE {
			class ValueComparator implements Comparator<Quantifier> {
				Map<Quantifier, Integer> base;

				public ValueComparator(Map<Quantifier, Integer> base) {
					this.base = base;
				}

				public int compare(Quantifier a, Quantifier b) {
					if (base.get(b).equals(base.get(a)))
						return 1;
					return base.get(a).compareTo(base.get(b));
				}
			}

			protected Comparator<Quantifier> getComparator(
					Map<Quantifier, Integer> base) {
				return new ValueComparator(base);
			}

			public short toMessage() {
				return 1;
			}

			@Override
			public String toString() {
				return "OBJ_MINIMIZE";
			}
		},
		SCOPING {
			protected Comparator<Quantifier> getComparator(
					Map<Quantifier, Integer> base) {
				return null;
			}

			public short toMessage() {
				return 2;
			}

			@Override
			public String toString() {
				return "OBJ_TEST";
			}
		};

		abstract protected Comparator<Quantifier> getComparator(
				Map<Quantifier, Integer> base);

		abstract public short toMessage();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		abstract public String toString();
	};

	/**
	 * 
	 * @param patterns
	 * @param newPattern
	 */
	protected static void addPattern(List<Pattern> patterns, Pattern newPattern) {
		for (Iterator<Pattern> i = patterns.iterator(); i.hasNext();) {
			Pattern crt = i.next();
			if (crt.equals(newPattern)) {
				if (!crt.quantifier_bound)
					crt.quantifiers.addAll(newPattern.quantifiers);
				crt.linkedAttributes.addAll(newPattern.linkedAttributes);
				crt.coefficients.get(0).putAll(newPattern.coefficients.get(0));
				crt.mappings.putAll(newPattern.mappings);
				return;
			} else {
				HashSet<Quantifier> tmp = new HashSet<Quantifier>(
						newPattern.quantifiers);
				tmp.retainAll(crt.quantifiers);
				if (!tmp.isEmpty()) {
					crt.linkedAttributes.add(newPattern.attribute.getName());
					newPattern.linkedAttributes.add(crt.attribute.getName());
				}
			}
		}
		patterns.add(newPattern);
	}

	/**
	 * 
	 * @param destPatterns
	 * @param toAdd
	 * @param quantIntersection
	 * @param applyScoping
	 */
	protected static void mergePatterns(List<Pattern> destPatterns,
			Pattern toAdd, Set<Quantifier> quantIntersection,
			boolean applyScoping) {
		ArrayList<Pattern> addQueue = new ArrayList<Pattern>();

		for (Iterator<Pattern> i = destPatterns.iterator(); i.hasNext();) {
			Pattern crt = i.next();

			if (crt.equals(toAdd)) {
				crt.mergePatterns(toAdd);
				return;
			} else if (quantIntersection != null
					&& !crt.attribute.equals(toAdd.attribute)) {
				HashSet<Quantifier> tmp = new HashSet<Quantifier>(
						quantIntersection);
				tmp.retainAll(crt.quantifiers);
				tmp.retainAll(crt.quantifiers);
				if (!tmp.isEmpty()) {
					crt.linkedAttributes.add(toAdd.attribute.getName());
					toAdd.linkedAttributes.add(crt.attribute.getName());
				}
				crt.coefficients.addAll(toAdd.coefficients);
				crt.mappings.putAll(toAdd.mappings);
			}
			if (applyScoping && crt.matchesScoping(toAdd)) {
				Pattern tmp1 = new Pattern(toAdd), tmp2 = new Pattern(toAdd);
				tmp1.quantifier_bound = true;
				tmp1.quantifiers.retainAll(crt.quantifiers);
				crt.dummy = true;
				addQueue.add(tmp1);
				addQueue.add(tmp2);
			}
		}

		if (!destPatterns.contains(toAdd))
			destPatterns.add(toAdd);
		for (Iterator<Pattern> it = addQueue.iterator(); it.hasNext();) {
			Pattern tmpToAdd = it.next();
			mergePatterns(destPatterns, tmpToAdd, quantIntersection,
					applyScoping);
		}
		return;
	}

	private Attribute attribute;

	private int boolNodeId;
	private ArrayList<HashMap<Quantifier, Integer>> coefficients;
	private boolean dummy;

	private int implicationId;

	private Set<String> linkedAttributes;

	private HashMap<Integer, ArrayList<Quantifier>> mappings;
	private Objective objective;

	private boolean quantifier_bound;

	private HashSet<Quantifier> quantifiers;

	/**
	 * 
	 * @param attribute
	 * @param quantifier
	 * @param boolNodeId
	 */
	public Pattern(Attribute attribute, Quantifier quantifier,
			int implicationId, int boolNodeId) {
		quantifiers = new HashSet<Quantifier>();
		mappings = new HashMap<Integer, ArrayList<Quantifier>>();
		this.attribute = attribute;
		quantifiers.add(quantifier);
		this.objective = Objective.SCOPING;
		quantifier_bound = true;
		this.implicationId = implicationId;
		this.boolNodeId = boolNodeId;
		linkedAttributes = new HashSet<String>();
		dummy = false;
	}

	/**
	 * 
	 * @param attribute
	 */
	public Pattern(Attribute attribute, Quantifier quantifier,
			Objective objective, int coefficient) {
		quantifiers = new HashSet<Quantifier>();
		mappings = new HashMap<Integer, ArrayList<Quantifier>>();
		this.attribute = attribute;
		this.objective = objective;
		quantifiers.add(quantifier);
		quantifier_bound = false;
		linkedAttributes = new HashSet<String>();
		coefficients = new ArrayList<HashMap<Quantifier, Integer>>();
		HashMap<Quantifier, Integer> tmp = new HashMap<Quantifier, Integer>();
		tmp.put(quantifier, coefficient);
		coefficients.add(tmp);
		dummy = false;
	}

	public Pattern(Pattern other) {
		attribute = other.attribute;
		objective = other.objective;
		quantifiers = new HashSet<Quantifier>(other.quantifiers);
		mappings = new HashMap<Integer, ArrayList<Quantifier>>(other.mappings);
		quantifier_bound = other.quantifier_bound;
		linkedAttributes = new HashSet<String>(other.linkedAttributes);
		coefficients = new ArrayList<HashMap<Quantifier, Integer>>(
				other.coefficients);
		dummy = other.dummy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pattern))
			return false;
		Pattern other = (Pattern) obj;
		boolean quantifiers_match = true;
		if (quantifier_bound)
			quantifiers_match = quantifiers.equals(other.quantifiers);
		return dummy == other.dummy
				&& quantifier_bound == other.quantifier_bound
				&& quantifiers_match
				&& attribute.getName().equals(other.attribute.getName())
				&& objective == other.objective;
	}

	/**
	 * 
	 * @return
	 */
	public Attribute getAttribute() {
		return attribute;
	}

	/**
	 * @return the boolNodeId
	 */
	public int getBoolNodeId() {
		return boolNodeId;
	}

	/**
	 * @return the implicationId
	 */
	public int getImplicationId() {
		return implicationId;
	}

	/**
	 * @return the linkedAttributes
	 */
	public Set<String> getLinkedAttributes() {
		return linkedAttributes;
	}

	/**
	 * @return the mappings
	 */
	public HashMap<Integer, ArrayList<Quantifier>> getMappings() {
		return mappings;
	}

	/**
	 * @return the objective
	 */
	public Objective getObjective() {
		return objective;
	}

	/**
	 * @return the quantifiers
	 */
	public HashSet<Quantifier> getQuantifiers() {
		return quantifiers;
	}

	/**
	 * @return the dummy
	 */
	public boolean isDummy() {
		return dummy;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isQuantifier_bound() {
		return quantifier_bound;
	}

	/**
	 * Maps quantifiers to pattern index.
	 * 
	 * @param id
	 */
	protected void mapQuantifiers(int id) {
		HashMap<Quantifier, Integer> baseMap = this.coefficients.get(0);

		TreeMap<Quantifier, Integer> treeMap = new TreeMap<Quantifier, Integer>(
				objective.getComparator(baseMap));
		treeMap.putAll(baseMap);

		mappings = new HashMap<Integer, ArrayList<Quantifier>>();
		mappings.put(id, new ArrayList<Quantifier>(treeMap.keySet()));

	}

	private boolean matchesScoping(Pattern other) {
		if (!(objective == Objective.SCOPING) || other.quantifier_bound)
			return false;
		HashSet<Quantifier> intersection = new HashSet<Quantifier>(quantifiers);
		intersection.retainAll(other.quantifiers);
		return intersection.size() != 0;
	}

	private void mergePatterns(Pattern other) {
		if (quantifier_bound)
			return;
		quantifiers.addAll(other.quantifiers);
		linkedAttributes.addAll(other.linkedAttributes);
		mappings.putAll(other.mappings);
		coefficients.addAll(other.coefficients);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		buf.append(objective + " " + attribute + quantifiers.size() + " ");
		buf.append(", C" + coefficients);
		buf.append(", M" + mappings);
		buf.append("]");
		return buf.toString();
	}
}
