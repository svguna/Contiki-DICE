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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.guna.dice.constraints.exceptions.InvalidTypeException;
import eu.guna.dice.constraints.exceptions.QuantifierInUseException;
import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;
import eu.guna.dice.constraints.operators.BoolOperator;
import eu.guna.dice.constraints.operators.ComparisonOperator;

/**
 * A node in the tree that represents the logical formulas. A node can be either
 * an boolean operator (and, or); leaves are either mathematical expressions
 * (i.e. comparisons).
 * 
 * The root of the tree can be either a constraint or a constraint alias.
 * 
 * @see net.dice.constraints.MathNode
 * @author Stefan Guna
 * 
 */
public class BoolNode {
	private class MathNodeId {
		private int id;

		private MathNodeId() {
			id = 0;
		}

		private int getId() {
			return id;
		}

		private void inc() {
			id++;
		}
	}

	enum ScopingType {
		MIXED, NO_SCOPING, ONLY_SCOPING;

		public static ScopingType combineScoping(ScopingType st1,
				ScopingType st2) {
			if (st1 == ONLY_SCOPING && st2 == ONLY_SCOPING)
				return ONLY_SCOPING;
			if (st1 == NO_SCOPING && st2 == NO_SCOPING)
				return NO_SCOPING;
			return MIXED;
		}
	}

	/**
	 * The type of the node. Can be leaf (constant or mathematical comparison)
	 * or an operator applied to two other boolean expressions.
	 */
	public enum Type {
		LEAF, LEAF_MATH, NODE;
	}

	/**
	 * Used in the pattern construction algorithm.
	 * {@link #triggerPatternAlgorithm()}
	 */
	private static int boolNodeId;

	/**
	 * Used in the pattern construction algorithm.
	 * {@link #triggerPatternAlgorithm()}
	 */
	private static int implicationId;

	private static Logger log = Logger.getLogger(BoolNode.class);

	/**
	 * This is the logical operator if the node is a mathematical comparison.
	 * The type of the node must be LEAF_MATH and the {@link #leftMathChild} and
	 * {@link #rightMathChild} must be set.
	 */
	private ComparisonOperator compOperator;

	/** The number of tolerance events to allow. */
	private int count;

	/**
	 * If this is a node in the logical expression tree, the left-hand side of
	 * the {@link #operator}.
	 */
	private BoolNode leftChild;

	/**
	 * If this node is an operator, the left-hand side of {@link #compOperator}.
	 */
	private MathNode leftMathChild;

	/** If a the node is logically negated. */
	private boolean negated = false;

	/** The logical operator between {@link #leftChild} and {@link #rightChild}. */
	private BoolOperator operator;

	/** Aggregation patterns */
	private ArrayList<Pattern> patterns;

	/** The period when the tolerance events are allowed. */
	private int period;

	/** The quantification list of the logical expression. */
	private Quantifications quantifications;

	/** The list of externalized quantifiers. */
	private List<Quantifier> quantifiers;

	/**
	 * If this is a node in the logical expression tree, the right-hand side of
	 * the {@link #operator}.
	 */
	private BoolNode rightChild;

	/**
	 * If this node is an operator, the right-hand side of {@link #compOperator}
	 * .
	 */
	private MathNode rightMathChild;

	/** The type of the node. */
	private Type type;

	/** If the node is a constant, the value of the node. */
	private Value value;

	public BoolNode(boolean value) {
		type = Type.LEAF;
		this.value = new Value(value);
		log.debug("new node: " + this);
	}

	/**
	 * Copy constructor. It is not a shallow copy, the entire tree is
	 * dublicated.
	 * 
	 * @param other
	 *            Where to copy the node from.
	 */
	protected BoolNode(BoolNode other) {
		compOperator = other.compOperator;
		if (other.leftChild != null) {
			leftChild = new BoolNode(other.leftChild);
			// assuming that we have a complete tree
			rightChild = new BoolNode(other.rightChild);
		}
		if (other.leftMathChild != null) {
			leftMathChild = new MathNode(other.leftMathChild);
			// assuming that we have a complete tree
			rightMathChild = new MathNode(other.rightMathChild);
		}
		negated = other.negated;
		operator = other.operator;
		quantifications = new Quantifications(other.quantifications);
		count = other.count;
		period = other.period;
		type = other.type;
		if (other.value != null)
			value = new Value(other.value);
	}

	/**
	 * Build a new node using a logical operator and two other logical nodes.
	 * 
	 * @param operator
	 *            The logical operator.
	 * @param leftChild
	 *            The left-hand side of the operator.
	 * @param rightChild
	 *            The righ-hand side of the operator.
	 */
	public BoolNode(BoolOperator operator, BoolNode leftChild,
			BoolNode rightChild) {
		type = Type.NODE;
		this.operator = operator;
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		log.debug("new node: " + this);
	}

	/**
	 * Build a node as a mathematical comparison.
	 * 
	 * @param operator
	 *            The comparison operator.
	 * @param leftMathChild
	 *            The left-hand side of the operator.
	 * @param rightMathChild
	 *            The right-hand side of the operator.
	 */

	public BoolNode(ComparisonOperator operator, MathNode leftMathChild,
			MathNode rightMathChild) {
		type = Type.LEAF_MATH;
		compOperator = operator;
		log.debug("inlining: " + leftMathChild);
		leftMathChild.inlineAdditive();
		log.debug("inline result: " + leftMathChild);
		leftMathChild.reduceAdditive();
		log.debug("reduce result: " + leftMathChild);
		leftMathChild.factor();
		log.debug("factor result: " + leftMathChild);
		this.leftMathChild = leftMathChild;
		this.rightMathChild = rightMathChild;
		log.debug("new node: " + this);
	}

	private void appendNegated(StringBuffer buf) {
		buf.append("          .negated = ");
		if (negated)
			buf.append("1,\n");
		else
			buf.append("0,\n");
		buf.append("        },\n");
	}

	/**
	 * Append the quantifiers to the parent of this node.
	 * 
	 * @param quantifiers
	 *            The list of quantifiers that appended to the upper level. This
	 *            parameters is changed; new quantifiers are appended at each
	 *            step.
	 * @param ids
	 *            Current quantification IDs.
	 * @param next_id
	 *            The ID to be set for the next found quantifier.
	 * @return the ID to be set for the next found quantifier
	 * @throws QuantifierNotFoundException
	 */
	private int externalizeQuantifiers(HashMap<String, Quantifier> quantifiers,
			int next_id) throws QuantifierNotFoundException {

		if (quantifications != null)
			for (int i = 0; i < quantifications.getQuantifiers().size(); i++) {
				Quantifier quantifier = quantifications.getQuantifiers().get(i);
				if (!quantifiers.containsKey(quantifier.getName())) {
					quantifiers.put(quantifier.getName(), quantifier);
					quantifier.setId(next_id++);
				} else
					throw new QuantifierInUseException(quantifier);
			}
		log.debug("ids: " + quantifiers);

		switch (type) {
		case LEAF:
			value.setQuantifier(quantifiers);
			break;
		case LEAF_MATH:
			leftMathChild.setQuantifiers(quantifiers);
			rightMathChild.setQuantifiers(quantifiers);
			break;
		case NODE:
			next_id = leftChild.externalizeQuantifiers(quantifiers, next_id);
			next_id = rightChild.externalizeQuantifiers(quantifiers, next_id);
		}
		return next_id;
	}

	public Set<BoolNode> extractDisjunctions() {
		Set<BoolNode> result = new HashSet<BoolNode>();
		extractDisjunctions(result);
		return result;
	}

	private void extractDisjunctions(Set<BoolNode> result) {
		if (operator != BoolOperator.OR) {
			result.add(this);
			return;
		}
		leftChild.extractDisjunctions(result);
		rightChild.extractDisjunctions(result);
	}

	public Set<Attribute> getAttributes() {
		Set<Attribute> result = new HashSet<Attribute>();

		switch (type) {
		case LEAF:
			if (value.getType().equals(Value.Type.ATTRIBUTE))
				result.add(value.getAttribute());
			break;
		case LEAF_MATH:
			result.addAll(leftMathChild.getAttributes());
			for (Attribute right : rightMathChild.getAttributes()) {
				boolean found = false;
				for (Attribute left : result)
					if (left.getHash() == right.getHash())
						found = true;
				if (!found)
					result.add(right);
			}
			break;
		case NODE:
			result.addAll(leftChild.getAttributes());
			for (Attribute right : rightChild.getAttributes()) {
				boolean found = false;
				for (Attribute left : result)
					if (left.getHash() == right.getHash())
						found = true;
				if (!found)
					result.add(right);
			}
			break;
		}

		return result;
	}

	/**
	 * Gets the boolean value of the node. Only valid if the node is a constant.
	 * 
	 * @return The boolean value
	 * @throws InvalidTypeException
	 *             When this node is not constant.
	 */
	public boolean getBool() throws InvalidTypeException {
		if (type != Type.LEAF)
			throw new InvalidTypeException();
		if (negated)
			return !value.getBool_value();
		return value.getBool_value();
	}

	/**
	 * @return the patterns
	 * @throws QuantifierNotFoundException
	 */
	public ArrayList<Pattern> getPatterns() throws QuantifierNotFoundException {
		if (patterns == null) {
			getQuantifiers();
			patterns = new ArrayList<Pattern>();
			implicationId = -1;
			boolNodeId = -1;
			getPatterns(patterns, new HashSet<Quantifier>(), false,
					isIntervalTest(), new MathNodeId());
		}
		log.debug("patterns: " + patterns);
		return patterns;
	}

	/**
	 * Gets the aggregation patterns.
	 * 
	 * @param patterns
	 *            Output parameter. The pattern are added to this list.
	 * @param quantifiers
	 *            The set of quantifiers used by this subtree.
	 * @param extractScoping
	 *            Flag indicating if the subtree is used in the scoping process.
	 * @param isIntervalTest
	 *            TODO
	 * @return Returns an indicator of whether the subtree is a scoped
	 *         expression.
	 */
	private ScopingType getPatterns(List<Pattern> patterns,
			Set<Quantifier> quantifiers, boolean extractScoping,
			boolean isIntervalTest, MathNodeId mathNodeId) {
		boolNodeId++;
		switch (type) {
		case LEAF:
			return ScopingType.NO_SCOPING;
		case LEAF_MATH:

			if (leftMathChild.depth() == 1) {
				try {
					Pattern pattern = leftMathChild.getValue().getAttribute()
							.getScopingPattern(implicationId, boolNodeId);
					Pattern.mergePatterns(patterns, pattern, null, false);
				} catch (InvalidTypeException e) {
					log.error(e.getMessage());
				}
				return ScopingType.ONLY_SCOPING;
			}
			List<Pattern> myPatterns = leftMathChild.getPatterns(quantifiers,
					compOperator == ComparisonOperator.GREATER,
					mathNodeId.getId());
			mathNodeId.inc();
			mergePatterns(patterns, myPatterns, null, false);

			return ScopingType.NO_SCOPING;
		case NODE:
			/* IMPLY: Mark the implication operator. */
			if (operator == BoolOperator.IMPLY) {
				implicationId++;
				extractScoping = true;
			}

			ArrayList<Pattern> leftPatterns = new ArrayList<Pattern>();
			ArrayList<Pattern> rightPatterns = new ArrayList<Pattern>();
			HashSet<Quantifier> leftQuantifiers = new HashSet<Quantifier>();
			HashSet<Quantifier> rightQuantifiers = new HashSet<Quantifier>();

			ScopingType leftScopingType = leftChild
					.getPatterns(leftPatterns, leftQuantifiers, extractScoping,
							isIntervalTest, mathNodeId);
			ScopingType rightScopingType = null;

			if (!isIntervalTest
					|| (rightChild.compOperator != null && rightChild.compOperator == ComparisonOperator.LOWER)) {
				rightScopingType = rightChild.getPatterns(rightPatterns,
						rightQuantifiers, extractScoping
								&& operator != BoolOperator.IMPLY,
						isIntervalTest, mathNodeId);

				HashSet<Quantifier> tmp = new HashSet<Quantifier>();
				if (operator == BoolOperator.AND
						|| operator == BoolOperator.IMPLY) {
					tmp = new HashSet<Quantifier>(leftQuantifiers);
					tmp.retainAll(rightQuantifiers);
				}
				if (!isIntervalTest)
					mergePatterns(
							leftPatterns,
							rightPatterns,
							tmp,
							operator == BoolOperator.IMPLY
									&& leftScopingType != ScopingType.NO_SCOPING);
			}
			if (isIntervalTest
					&& rightChild.compOperator == ComparisonOperator.LOWER)
				mergePatterns(patterns, rightPatterns, null, false);
			else
				// IMPLY: just append the patterns
				mergePatterns(patterns, leftPatterns, null, false);

			quantifiers.addAll(leftQuantifiers);
			if (isIntervalTest)
				return ScopingType.NO_SCOPING;

			quantifiers.addAll(rightQuantifiers);
			/* TODO IMPLY: No expressions such as "A -> B -> C are allowed". */
			if (!extractScoping || operator == BoolOperator.IMPLY)
				return ScopingType.NO_SCOPING;
			return ScopingType
					.combineScoping(leftScopingType, rightScopingType);
		}
		throw new RuntimeException("missing case!");
	}

	/**
	 * Returns the list of quantifiers. Computes it if it has not been computed
	 * yet.
	 * 
	 * @return the list of quantifiers
	 * @throws QuantifierNotFoundException
	 */
	public List<Quantifier> getQuantifiers() throws QuantifierNotFoundException {
		if (this.quantifiers != null)
			return this.quantifiers;
		log.debug("triggering new computation of quantifiers");
		HashMap<String, Quantifier> quantifiers = new HashMap<String, Quantifier>();
		externalizeQuantifiers(quantifiers, 0);
		this.quantifiers = new ArrayList<Quantifier>(quantifiers.values());
		return this.quantifiers;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Check whether this root represents an interval test.
	 * 
	 * @return True if the node represents an interval test, false otherwise.
	 * @throws QuantifierNotFoundException
	 *             If a quantifier used in the expression was not declared.
	 */
	public boolean isIntervalTest() throws QuantifierNotFoundException {
		if (type != Type.NODE || operator != BoolOperator.OR
				|| leftChild.type != Type.LEAF_MATH
				|| leftChild.type != Type.LEAF_MATH)
			return false;

		ComparisonOperator leftComp = leftChild.compOperator, rightComp = rightChild.compOperator;
		if (leftComp == null || rightComp == null)
			return false;

		BoolNode lowerNode = null, greaterNode = null;
		if (leftComp == ComparisonOperator.LOWER)
			lowerNode = leftChild;
		if (leftComp == ComparisonOperator.GREATER)
			greaterNode = leftChild;
		if (rightComp == ComparisonOperator.LOWER)
			lowerNode = rightChild;
		if (rightComp == ComparisonOperator.GREATER)
			greaterNode = rightChild;
		if (lowerNode == null || greaterNode == null)
			return false;

		List<Value> lowerFreeTerms = lowerNode.leftMathChild.extractFreeTerms();
		List<Value> greaterFreeTerms = greaterNode.leftMathChild
				.extractFreeTerms();
		if (lowerFreeTerms.size() != 1 || greaterFreeTerms.size() != 1)
			return false;

		int lowerValue, greaterValue;
		try {
			lowerValue = lowerFreeTerms.get(0).getInt_value();
			greaterValue = greaterFreeTerms.get(0).getInt_value();
		} catch (InvalidTypeException e) {
			return false;
		}
		if (lowerValue > greaterValue)
			return false;

		List<MathNode> lowerTerms = lowerNode.leftMathChild
				.extractNonFreeTerms();
		List<MathNode> greaterTerms = greaterNode.leftMathChild
				.extractNonFreeTerms();
		return MathNode.compareTermsList(lowerTerms, greaterTerms);
	}

	/**
	 * @return the negated
	 */
	protected boolean isNegated() {
		return negated;
	}

	public boolean isType2() {
		if (leftMathChild != null && leftMathChild.getOperator() != null)
			return true;
		if (rightMathChild != null && rightMathChild.getOperator() != null)
			return true;
		if (leftChild != null && leftChild.isType2())
			return true;
		if (rightChild != null && rightChild.isType2())
			return true;
		return false;
	}

	private int leafMathToContiki(boolean simpleType1, StringBuffer buf,
			int index) {
		index = leftMathChild.toContiki(buf, index);

		if (simpleType1) {
			buf.append("        { .type = OPERATOR,\n");
			buf.append("          .data.op_code = " + compOperator.toContiki()
					+ ",\n");
			appendNegated(buf);
		}

		index = rightMathChild.toContiki(buf, index);

		if (simpleType1)
			return index;

		buf.append("        { .type = OPERATOR,\n");
		buf.append("          .data.op_code = " + compOperator.toContiki()
				+ ",\n");
		appendNegated(buf);
		index++;

		return index;
	}

	private void mergePatterns(List<Pattern> destination, List<Pattern> source,
			Set<Quantifier> quantIntersection, boolean applyScoping) {
		for (Iterator<Pattern> i = source.iterator(); i.hasNext();) {
			Pattern from = i.next();
			Pattern.mergePatterns(destination, from, quantIntersection,
					applyScoping);
		}
	}

	/**
	 * Negates the boolean expression.
	 */
	protected void negate() {
		negated = !negated;
		log.debug("negating node: " + this);
	}

	private int nodeToContiki(boolean simpleType1, StringBuffer buf, int index) {
		index = leftChild.toContiki(simpleType1, buf, index);
		index = rightChild.toContiki(simpleType1, buf, index);

		if (simpleType1) {
			if (!operator.equals(BoolOperator.AND))
				throw new UnsupportedOperationException(
						"The constraint is not in the supported form!");
			return index;
		}

		buf.append("        { .type = OPERATOR,\n");
		buf.append("          .data.op_code = " + operator.toContiki() + ",\n");
		appendNegated(buf);
		index++;

		return index;
	}

	/**
	 * @param quantifications
	 *            the quantifications to set
	 */
	protected void setQuantifications(Quantifications quantifications) {
		if (this.quantifications != null)
			this.quantifications.merge(quantifications);
		else
			this.quantifications = quantifications;
	}

	/**
	 * Appends the assignment code for this node into the buffer.
	 * 
	 * @param simpleType1
	 *            TODO
	 * @param buf
	 *            The buffer to append to
	 * 
	 * @return the next node index
	 */
	public int toContiki(boolean simpleType1, StringBuffer buf) {
		return toContiki(simpleType1, buf, 0);
	}

	/**
	 * Appends the assignment code for this node into the buffer.
	 * 
	 * @param simpleType1
	 *            TODO
	 * @param buf
	 *            The buffer to append to
	 * @param index
	 *            The current node index
	 * 
	 * @return the next node index
	 */
	private int toContiki(boolean simpleType1, StringBuffer buf, int index) {

		switch (type) {
		case LEAF:
			index = value.toContiki(buf, index);
			appendNegated(buf);
			return index;
		case LEAF_MATH:
			return leafMathToContiki(simpleType1, buf, index);
		case NODE:
			return nodeToContiki(simpleType1, buf, index);
		}
		return index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String quantifiers = "( ";
		if (this.quantifiers != null) {
			for (Iterator<Quantifier> it = this.quantifiers.iterator(); it
					.hasNext();) {
				Quantifier q = it.next();
				quantifiers += q.toString();
			}
		}
		quantifiers += ")";

		switch (type) {
		case LEAF:
			try {
				return quantifiers + ((getBool()) ? "true" : "false");
			} catch (InvalidTypeException e) {
				return "invalid node";
			}
		case LEAF_MATH:
			return quantifiers + ((negated) ? "not " : "") + "["
					+ leftMathChild + " " + compOperator + " " + rightMathChild
					+ "]";
		case NODE:
			return quantifiers + ((negated) ? "not " : "") + "[" + leftChild
					+ " " + operator + " " + rightChild + "]";
		}
		return "invalid node";
	}
}
