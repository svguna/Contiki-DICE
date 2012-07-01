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
import eu.guna.dice.constraints.exceptions.QuantifierNotFoundException;
import eu.guna.dice.constraints.operators.MathOperator;

/**
 * This class represent a node in mathematical expression tree.
 * 
 * A node can hold a value (node attribute or a constant) or an operator. In the
 * later, the node must have two children. The leaves of the tree must be
 * values.
 * 
 * @author Stefan Guna
 * 
 */
public class MathNode {
	/**
	 * An Optimizer implementation for additive operators (+ and -)
	 * 
	 * @author Stefan Guna
	 * 
	 */
	private static class AdditiveOptimizer implements Optimizer {
		public boolean canCombine(MathOperator operator) {
			return true;
		}

		public MathOperator getNegativeOperator() {
			return MathOperator.MINUS;
		}

		public int getNegativeValue(int value) {
			return -value;
		}

		public int getNeutral() {
			return 0;
		}

		public MathOperator getPositiveOperator() {
			return MathOperator.PLUS;
		}

		public MathOperator getWorkingOperator() {
			return getPositiveOperator();
		}

		public boolean negationInterested() {
			return true;
		}

		public boolean operatorMatch(MathOperator operator) {
			return operator.isAdditive();
		}

		public boolean rightAligned() {
			// TODO Auto-generated method stub
			return false;
		}
	};

	/**
	 * An optimizer implementation for the division operator. This is used to
	 * extract the 1 / x term out of a factor. Do not use this for
	 * linearization.
	 * 
	 * @author Stefan Guna
	 * 
	 */
	private static class DivisionOptimizer implements Optimizer {
		public boolean canCombine(MathOperator operator) {
			return operator == MathOperator.DIV;
		}

		public MathOperator getNegativeOperator() {
			return null;
		}

		public int getNegativeValue(int value) {
			return value;
		}

		public int getNeutral() {
			return 1;
		}

		public MathOperator getPositiveOperator() {
			return MathOperator.DIV;
		}

		public MathOperator getWorkingOperator() {
			return getPositiveOperator();
		}

		public boolean negationInterested() {
			return false;
		}

		public boolean operatorMatch(MathOperator operator) {
			return operator.isMultiplicative();
		}

		public boolean rightAligned() {
			return true;
		}
	};

	/**
	 * Class that embeds two optimizations algorithms. The first algorithm make
	 * the math expression tree left recursive, i.e. for any node in the tree,
	 * the right node is a leaf. The second algorithm reduces all the constants
	 * in the expressions, i.e. from 5 + a + 3 to a + 8.
	 * 
	 * The algorithms are controlled by an
	 * {@code dice.compiler.constraints.MathNode.Optimizer}, which prevents
	 * non-free terms to be reduced with free terms.
	 * 
	 * @see dice.compiler.constraints.MathNode.Optimizer
	 * 
	 * @author Stefan Guna
	 * 
	 */
	private class MathOptimizer {
		private Optimizer optimizer;

		protected MathOptimizer(Optimizer optimizer) {
			this.optimizer = optimizer;
		}

		/**
		 * Combines two values
		 * 
		 * @param left
		 * @param right
		 * @return
		 * @throws InvalidTypeException
		 */
		private Value combineValues(Value left, Value right)
				throws InvalidTypeException {
			int l = left.getInt_value();
			int r = right.getInt_value();

			if (!optimizer.canCombine(operator))
				throw new InvalidTypeException();

			switch (operator) {

			case MINUS:
				return new Value((int) l - r);
			case MOD:
				return new Value((int) l % r);
			case DIV:
				/* fall through: special case to obtain rational number */
			case MUL:
				return new Value((int) l * r);
			case PLUS:
				return new Value((int) l + r);
			}
			return new Value(optimizer.getNeutral());
		}

		/**
		 * Finishes the extraction of free terms. This is called to extract
		 * valuable information at the end of the recursivity loop.
		 * 
		 * @param carry
		 * @return
		 */
		private MathNode endReduce(Value carry) {
			int leftInt = 0;

			if (optimizer.rightAligned() && optimizer.canCombine(operator)) {
				int rightInt;
				try {
					rightInt = rightChild.getInt();
				} catch (InvalidTypeException e) {
					if (!optimizer.negationInterested())
						negated = false;
					return getMyself();
				}
				try {
					if (operator == optimizer.getNegativeOperator())
						carry.setInt_value(optimizer.getNegativeValue(rightInt));
					else
						carry.setInt_value(rightInt);
				} catch (InvalidTypeException e1) {
					// this will never happen
					e1.printStackTrace();
				}
				if (isNegated())
					leftChild.negate();
				if (!optimizer.negationInterested())
					leftChild.negated = false;
				return leftChild;
			}

			try {
				leftInt = leftChild.getInt();
			} catch (InvalidTypeException e) {
				int rightInt;
				try {
					if (optimizer.canCombine(operator))
						rightInt = rightChild.getInt();
					else {
						if (!optimizer.negationInterested())
							negated = false;
						return getMyself();
					}
				} catch (InvalidTypeException e1) {
					if (!optimizer.negationInterested())
						negated = false;
					return getMyself();
				}
				try {
					if (operator == optimizer.getNegativeOperator())
						carry.setInt_value(optimizer.getNegativeValue(rightInt));
					else
						carry.setInt_value(rightInt);
				} catch (InvalidTypeException e1) {
					// this will never happen
					e1.printStackTrace();
				}
				if (isNegated())
					leftChild.negate();
				if (!optimizer.negationInterested())
					leftChild.negated = false;
				return leftChild;
			}

			try {
				carry.setInt_value(leftInt);
			} catch (InvalidTypeException e) {
				// this will never happen
				e.printStackTrace();
			}
			if (negated)
				rightChild.negate();
			if (operator == optimizer.getNegativeOperator())
				rightChild.negate();
			if (!optimizer.negationInterested())
				rightChild.negated = false;
			return rightChild;
		}

		/**
		 * Make the expression left recursive.
		 */
		protected void inline() {
			if (type == Type.LEAF || !optimizer.operatorMatch(operator)) {
				optimizeMultiplicative(getMyself());
				return;
			}

			if (leftChild == null && rightChild == null)
				return;
			leftChild.new MathOptimizer(optimizer).inline();
			rightChild.new MathOptimizer(optimizer).inline();

			try {
				if (!optimizer.operatorMatch(rightChild.operator)) {
					optimizeMultiplicative(rightChild);
					return;
				}
			} catch (NullPointerException e) {
				return;
			}

			MathNode iterator;
			boolean negIterator;
			MathOperator tmpOp;
			for (iterator = rightChild.leftChild, negIterator = false; iterator.type != Type.LEAF
					&& optimizer.operatorMatch(iterator.operator); iterator = iterator.leftChild) {
				if (iterator.negated && optimizer.negationInterested())
					negIterator = !negIterator;
				tmpOp = (negIterator) ? iterator.operator.inverseOperator()
						: iterator.operator;
				leftChild = new MathNode(
						(operator != tmpOp) ? optimizer.getNegativeOperator()
								: optimizer.getPositiveOperator(), leftChild,
						iterator.rightChild);
			}

			tmpOp = (negIterator) ? operator.inverseOperator() : operator;
			leftChild = new MathNode(tmpOp, leftChild, iterator);

			tmpOp = (rightChild.negated && optimizer.negationInterested()) ? rightChild.operator
					.inverseOperator() : rightChild.operator;
			operator = (operator != tmpOp) ? optimizer.getNegativeOperator()
					: optimizer.getPositiveOperator();
			rightChild = rightChild.rightChild;

		}

		private void optimizeMultiplicative(MathNode node) {
			if (node.type != Type.LEAF && node.operator.isMultiplicative()) {
				node.negated = node.multiplicativeTreeNegated();
				node.new MathOptimizer(new MultiplicativeOptimizer()).inline();
				node.new MathOptimizer(new DivisionOptimizer()).reduce();
				node.new MathOptimizer(new MultiplicationOptimizer()).reduce();
			}
		}

		/**
		 * Extracts the free terms of the tree.
		 * 
		 */
		protected void reduce() {
			if (leftChild.type == Type.LEAF
					|| !optimizer.operatorMatch(leftChild.operator))
				return;

			Value leftCarry = new Value(optimizer.getNeutral());
			leftChild = leftChild.new MathOptimizer(optimizer)
					.reduce(leftCarry);

			Value temp = null;
			try {
				temp = combineValues(leftCarry, rightChild.getValue());
			} catch (InvalidTypeException e) {
				try {
					if (leftCarry.getInt_value() == optimizer.getNeutral())
						return;
					MathNode tmpNode = new MathNode(leftCarry);
					MathNode newLeft = new MathNode(
							optimizer.getWorkingOperator(), leftChild, tmpNode);
					leftChild = newLeft;
					return;
				} catch (InvalidTypeException e1) {
					// this will never happen
					e1.printStackTrace();
				}
			}

			try {
				operator = optimizer.getWorkingOperator();
				rightChild.getValue().setInt_value(temp.getInt_value());
			} catch (InvalidTypeException e) {
				// this will never happen
				e.printStackTrace();
			}
		}

		/**
		 * Extracts the free term of the given sub-tree.
		 * 
		 * @param carry
		 *            The free term, an output parameter.
		 * @return The sub-tree changed in such a way that it contains no free
		 *         terms.
		 */
		private MathNode reduce(Value carry) {
			if (leftChild.type == Type.LEAF
					|| !optimizer.operatorMatch(leftChild.operator))
				return endReduce(carry);

			Value leftCarry = new Value(optimizer.getNeutral());
			leftChild = leftChild.new MathOptimizer(optimizer)
					.reduce(leftCarry);

			Value temp;
			try {
				temp = combineValues(leftCarry, rightChild.getValue());
			} catch (InvalidTypeException e) {
				// Happens only if right is not a leaf or is an attribute.
				try {
					if (isNegated())
						carry.setInt_value(optimizer.getNegativeValue(leftCarry
								.getInt_value()));
					else
						carry.setInt_value(leftCarry.getInt_value());
				} catch (InvalidTypeException e1) {
					// This cannot happen.
					e1.printStackTrace();
				}
				if (!optimizer.negationInterested())
					negated = false;
				if (!optimizer.negationInterested())
					negated = false;
				return getMyself();
			}

			try {
				if (isNegated())
					carry.setInt_value(optimizer.getNegativeValue(temp
							.getInt_value()));
				else
					carry.setInt_value(temp.getInt_value());
			} catch (InvalidTypeException e1) {
				// This cannot happen.
				e1.printStackTrace();
			}
			if (negated)
				leftChild.negate();
			if (!optimizer.negationInterested())
				leftChild.negated = false;
			return leftChild;
		}
	}

	/**
	 * An optimizer implementation for the multiplication operator. Used to
	 * extract the x / free term. Do not use this for linearization.
	 * 
	 * @author Stefan Guna
	 * 
	 */
	private static class MultiplicationOptimizer implements Optimizer {
		public boolean canCombine(MathOperator operator) {
			return operator == MathOperator.MUL;
		}

		public MathOperator getNegativeOperator() {
			return null;
		}

		public int getNegativeValue(int value) {
			return value;
		}

		public int getNeutral() {
			return 1;
		}

		public MathOperator getPositiveOperator() {
			return MathOperator.MUL;
		}

		public MathOperator getWorkingOperator() {
			return getPositiveOperator();
		}

		public boolean negationInterested() {
			return false;
		}

		public boolean operatorMatch(MathOperator operator) {
			return operator.isMultiplicative();
		}

		public boolean rightAligned() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	/**
	 * An optimizer implementation used to linearize multiplicative expressions.
	 * Do not use this for free term extraction.
	 * 
	 * @author Stefan Guna
	 * 
	 */
	private static class MultiplicativeOptimizer implements Optimizer {
		public boolean canCombine(MathOperator operator) {
			return false;
		}

		public MathOperator getNegativeOperator() {
			return MathOperator.DIV;
		}

		public int getNegativeValue(int value) {
			return value;
		}

		public int getNeutral() {
			return 1;
		}

		public MathOperator getPositiveOperator() {
			return MathOperator.MUL;
		}

		public MathOperator getWorkingOperator() {
			return null;
		}

		public boolean negationInterested() {
			return false;
		}

		public boolean operatorMatch(MathOperator operator) {
			return operator.isMultiplicative();
		}

		public boolean rightAligned() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	/**
	 * Controls the optimization algorithms implemented by {@code MathOptimizer}
	 * .
	 * 
	 * @author Stefan Guna
	 * 
	 */
	private static interface Optimizer {
		/**
		 * Tells whether two values can be combined by this operator.
		 * 
		 * @param operator
		 * @return
		 */
		boolean canCombine(MathOperator operator);

		/**
		 * Returns the operator which would give a negative correlation.
		 * 
		 * @return
		 */
		MathOperator getNegativeOperator();

		/**
		 * Given a value, returns the negative. For instance, if the Optimizer
		 * is a multiplicative one, it should return 1./value. If the optimizer
		 * is additive, it should return -value.
		 * 
		 * @param value
		 * @return
		 */
		int getNegativeValue(int value);

		/**
		 * Get the neutral element with respect to the optimizer.
		 * 
		 * @return
		 */
		int getNeutral();

		/**
		 * Returns the operator which would give a positive correlation.
		 * 
		 * @return
		 */
		MathOperator getPositiveOperator();

		/**
		 * Returns the operator which is used when reducing values.
		 * 
		 * @return
		 */
		MathOperator getWorkingOperator();

		/**
		 * Tells whether negation are kept along the tree when optimizing.
		 * 
		 * @return
		 */
		boolean negationInterested();

		/**
		 * Tells whether this optimizer addresses the given operator.
		 * 
		 * @param operator
		 * @return
		 */
		boolean operatorMatch(MathOperator operator);

		/**
		 * Tells whether this optimizer is right aligned.
		 * 
		 * @return
		 */
		boolean rightAligned();
	}

	protected enum Type {
		LEAF, NODE;
	};

	private static Logger log = Logger.getLogger(MathNode.class);

	/**
	 * Compares two lists of of expressions, each expression having only
	 * multiplication and divisions.
	 * 
	 * @param l1
	 * @param l2
	 * @return True if the expressions are equivalent, false otherwise.
	 */
	protected static boolean compareTermsList(List<MathNode> l1,
			List<MathNode> l2) {
		if (l1.size() != l2.size())
			return false;
		for (Iterator<MathNode> it1 = l1.iterator(); it1.hasNext();) {
			MathNode node1 = it1.next();
			boolean foundEqual = false;
			for (Iterator<MathNode> it2 = l2.iterator(); it2.hasNext();) {
				MathNode node2 = it2.next();
				if (node1.productMatches(node2)) {
					foundEqual = true;
					break;
				}
			}
			if (!foundEqual)
				return false;
		}
		return true;
	};

	/** If this is not a leaf, the left-hand side of {@link #operator}. */
	private MathNode leftChild;

	/** If the node is a negative value. */
	private boolean negated = false;

	private MathOperator operator;

	/** If this is not a leaf, the right hand-side of {@link #operator}. */
	private MathNode rightChild;

	/** Flag to indicate that the preceding operator is minus or division. */
	private boolean rightOfNegation = false;

	/** The type of the node (leaf if it is a constant or an attribute). */
	private Type type;

	/** The value of leaf nodes. */
	private Value value;

	/**
	 * Copy constructor. Creates a full copy of the tree (it is not a shallow
	 * copy).
	 * 
	 * @param other
	 *            The node to copy data from.
	 */
	protected MathNode(MathNode other) {
		replace(other, false);
	}

	/**
	 * @param operator
	 * @param leftChild
	 * @param rightChild
	 */
	public MathNode(MathOperator operator, MathNode leftChild,
			MathNode rightChild) {
		type = Type.NODE;
		this.operator = operator;
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		log.debug("new node: " + this);
	}

	/**
	 * @param value
	 */
	public MathNode(Value value) {
		type = Type.LEAF;
		this.value = value;
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
	 * Return a boolean indicating whether this node can be used to be factored.
	 * 
	 * @return
	 */

	private boolean canFactor() {
		if (type == Type.LEAF)
			return value.getType() == Value.Type.ATTRIBUTE;
		int leftDepth = leftChild.depth();
		int rightDepth = rightChild.depth();
		if ((leftDepth > 0 && rightDepth > 0)
				|| (rightDepth == 1 && operator == MathOperator.DIV)) {
			// TODO non-linear expression, so there is no point
		}
		return leftDepth == 1 || rightDepth == 1;
	}

	/**
	 * Factors the right child of this node with a given node.
	 * 
	 * @param other
	 * @param otherOp
	 * @return
	 */
	private boolean combineFactors(MathNode other, MathOperator otherOp) {
		if (!other.canFactor())
			return false;
		MathNode myAtt = rightChild.leftChild, myCoeff = rightChild.rightChild;
		MathNode otherAtt = other.rightChild, otherCoeff = other.leftChild;

		if (rightChild.type != Type.LEAF && rightChild.rightChild.depth() == 1) {
			myAtt = rightChild.rightChild;
			myCoeff = rightChild.leftChild;
		}

		if (other.type == Type.LEAF) {
			otherAtt = other;
			otherCoeff = new MathNode(new Value(1));
		} else if (other.leftChild.depth() == 1) {
			otherAtt = other.leftChild;
			otherCoeff = other.rightChild;
		}

		if (rightChild.type == Type.LEAF)
			myAtt = rightChild;

		try {
			if (!myAtt.getValue().equals(otherAtt.getValue()))
				return false;
		} catch (InvalidTypeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (rightChild.type == Type.LEAF) {
			myCoeff = new MathNode(new Value(1));
			rightChild = new MathNode(MathOperator.MUL, myCoeff, rightChild);
		}

		try {
			int signMatch = 1;

			if (negated)
				signMatch *= -1;
			if (rightChild.negated)
				signMatch *= -1;
			if (myCoeff.negated)
				signMatch *= -1;
			if (myAtt.negated)
				signMatch *= -1;
			if (otherCoeff.negated)
				signMatch *= -1;
			if (otherAtt.negated)
				signMatch *= -1;
			if ((otherOp == null && operator == MathOperator.MINUS)
					|| (otherOp != null && operator != otherOp))
				signMatch *= -1;
			if (signMatch == -1)
				myCoeff.value.setInt_value(myCoeff.getInt()
						- otherCoeff.getInt());
			else
				myCoeff.value.setInt_value(myCoeff.getInt()
						+ otherCoeff.getInt());
		} catch (InvalidTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Computes the depth of this node. Constant values do not count in the
	 * depth calculation, while attributes do.
	 * 
	 * @return The depth
	 */
	public int depth() {
		if (type == Type.NODE) {
			int left_depth = leftChild.depth();
			int right_depth = leftChild.depth();
			if (left_depth > right_depth)
				return 1 + left_depth;
			return 1 + right_depth;
		}
		return value.depth();
	}

	/**
	 * Extract the free terms in this expressions. Please note that if the free
	 * term is in a multiplication / division, this term will be skipped. A non
	 * free term has the form attribute@quantifier, while a free term is a
	 * constant.
	 * 
	 * @return A list containing the free terms.
	 */
	protected List<Value> extractFreeTerms() {
		ArrayList<Value> result = new ArrayList<Value>();
		if (type == Type.LEAF) {
			if (value.getType() != Value.Type.ATTRIBUTE) {
				result.add(value);
				return result;
			}
			return result;
		}
		if (operator == MathOperator.PLUS || operator == MathOperator.MINUS) {
			result.addAll(leftChild.extractFreeTerms());
			result.addAll(rightChild.extractFreeTerms());
		}
		return result;
	}

	/**
	 * Extract the non free terms in this tree separated by plus and minus. A
	 * non free term has the form attribute@quantifier, while a free term is a
	 * constant.
	 * 
	 * @return A list containing the subtree of non free terms.
	 */
	protected List<MathNode> extractNonFreeTerms() {
		ArrayList<MathNode> result = new ArrayList<MathNode>();
		if (type == Type.LEAF) {
			if (value.getType() == Value.Type.ATTRIBUTE)
				result.add(this);
			return result;
		}
		if (operator != MathOperator.PLUS && operator != MathOperator.MINUS) {
			result.add(this);
			return result;
		}
		result.addAll(leftChild.extractNonFreeTerms());
		if (operator == MathOperator.MINUS || operator == MathOperator.DIV)
			rightChild.rightOfNegation = true;
		result.addAll(rightChild.extractNonFreeTerms());
		return result;
	}

	/**
	 * Extract all terms in this expression.
	 * 
	 * @return A list containing all terms in this expression.
	 */
	protected List<MathNode> extractTerms() {
		ArrayList<MathNode> result = new ArrayList<MathNode>();
		if (type == Type.LEAF) {
			result.add(this);
			return result;
		}
		result.addAll(leftChild.extractTerms());
		if (operator == MathOperator.MINUS || operator == MathOperator.DIV)
			rightChild.rightOfNegation = true;
		result.addAll(rightChild.extractTerms());
		return result;
	}

	/**
	 * Factors a linear expression, that is, instead of 5a + 6a, it simplifies
	 * it to 11a.
	 * 
	 */
	public void factor() {
		if (type == Type.LEAF)
			return;
		if (!rightChild.canFactor())
			leftChild.factor();

		MathNode prevIterator = MathNode.this;
		MathNode iterator = leftChild;

		while (true) {

			if (iterator.type == Type.LEAF || !iterator.operator.isAdditive()) {
				if (!combineFactors(iterator, null))
					break;

				prevIterator.replace(prevIterator.rightChild,
						prevIterator.operator == MathOperator.MINUS);
				break;
			}

			if (combineFactors(iterator.rightChild, iterator.operator))
				prevIterator.leftChild = iterator.leftChild;
			else
				prevIterator = iterator;

			iterator = iterator.leftChild;
		}

	}

	private int getCoefficient() throws InvalidTypeException {
		try {
			return leftChild.getInt();
		} catch (InvalidTypeException e) {
		}
		return rightChild.getInt();
	}

	/**
	 * Finds all existential quantifiers referenced by the subtree of this node.
	 * 
	 * @return A set of existential quantifiers.
	 */
	protected Set<Quantifier> getExistentialQuantifiers() {
		Set<Quantifier> result = new HashSet<Quantifier>();
		switch (type) {
		case LEAF:
			if (value.getType() != Value.Type.ATTRIBUTE)
				break;
			Quantifier quantifier = value.getAttribute().getQuantifier();
			if (quantifier.getType() == Quantifier.Type.EXISTENTIAL)
				result.add(quantifier);
			break;
		case NODE:
			result.addAll(leftChild.getExistentialQuantifiers());
			result.addAll(rightChild.getExistentialQuantifiers());
			break;
		}
		return result;
	}

	/**
	 * Returns the float value associated with this node. If this node hosts an
	 * integer value, it casts it to a float.
	 * 
	 * @return the float value
	 * @throws InvalidTypeException
	 *             if the node is a boolean, an attribute or is not a leaf.
	 */
	public float getFloat() throws InvalidTypeException {
		if (type != Type.LEAF)
			throw new InvalidTypeException();
		float result;
		try {
			result = value.getFloat_value();
		} catch (InvalidTypeException e) {
			result = value.getInt_value();
		}
		if (negated)
			result *= -1;
		return result;
	}

	/**
	 * Returns the integer value associated with this node.
	 * 
	 * @return the integer value
	 * @throws InvalidTypeException
	 *             if the node is not an integer leaf
	 */
	public int getInt() throws InvalidTypeException {
		if (type != Type.LEAF)
			throw new InvalidTypeException();
		int result = value.getInt_value();
		if (negated)
			result *= -1;
		return result;
	}

	private MathNode getMyself() {
		return this;
	}

	/**
	 * @return the operator
	 */
	protected MathOperator getOperator() {
		return operator;
	}

	/**
	 * Build the aggregation patterns for this node.
	 * 
	 * @param maxHistory
	 *            The maximization history for attributes. This argument is
	 *            changed.
	 * @param minHistory
	 *            The minimization history for attributes. This argument is
	 *            changed.
	 * @param patterns
	 *            The patterns for attributes. This argument is changed and is
	 *            part of the actual result.
	 * @param minimize
	 *            Inverses from maximization to minimization.
	 * @param quantifiers
	 *            List of "externalized" quantifiers.
	 */
	private void getPatterns(HashMap<String, List<Integer>> maxHistory,
			HashMap<String, List<Integer>> minHistory, List<Pattern> patterns,
			boolean minimize, Set<Quantifier> quantifiers,
			HashMap<Quantifier, Integer> coefficients, int coefficient) {

		if (negated)
			minimize = !minimize;

		switch (type) {
		case NODE:
			try {
				if (canFactor())
					coefficient = getCoefficient();
			} catch (InvalidTypeException e) {
				coefficient = 1;
			}
			leftChild.getPatterns(maxHistory, minHistory, patterns, minimize,
					quantifiers, coefficients, coefficient);
			boolean rightMinimize = (operator.minimizes()) ? !minimize
					: minimize;
			rightChild.getPatterns(maxHistory, minHistory, patterns,
					rightMinimize, quantifiers, coefficients, coefficient);
			break;
		case LEAF:
			value.getPattern(maxHistory, minHistory, patterns, minimize,
					quantifiers, coefficient);
			break;
		}
	}

	/**
	 * Wrapper for the {@link #getPatterns(HashMap, HashMap, List, boolean)}
	 * 
	 * @param patterns
	 *            Output parameter. Appends all the patterns extracted from the
	 *            math expression to this list.
	 * @param quantifiers
	 *            List of "externalized" quantifiers.
	 */
	protected List<Pattern> getPatterns(Set<Quantifier> quantifiers,
			boolean minimize, int id) {
		ArrayList<Pattern> patterns = new ArrayList<Pattern>();
		HashMap<String, List<Integer>> maxHistory = new HashMap<String, List<Integer>>();
		HashMap<String, List<Integer>> minHistory = new HashMap<String, List<Integer>>();
		HashMap<Quantifier, Integer> coefficients = new HashMap<Quantifier, Integer>();
		getPatterns(maxHistory, minHistory, patterns, minimize, quantifiers,
				coefficients, 1);
		for (Iterator<Pattern> itPattern = patterns.iterator(); itPattern
				.hasNext();) {
			Pattern pattern = itPattern.next();
			pattern.mapQuantifiers(id);
		}
		log.debug("patterns: " + patterns);
		return patterns;
	}

	/**
	 * @return the type
	 */
	protected Type getType() {
		return type;
	}

	/**
	 * @return the value
	 * @throws InvalidTypeException
	 */
	protected Value getValue() throws InvalidTypeException {
		if (type != Type.LEAF)
			throw new InvalidTypeException();
		return value;
	}

	/**
	 * Inlines this tree, i.e. it makes it left-recursive.
	 * 
	 * @see MathOptimizer
	 */
	public void inlineAdditive() {
		new MathOptimizer(new AdditiveOptimizer()).inline();
	}

	/**
	 * @return the negated
	 */
	protected boolean isNegated() {
		return negated;
	}

	/** Returns the sign of a multiplicative expression. */
	private boolean multiplicativeTreeNegated() {
		boolean result = negated;
		if (type != Type.LEAF) {
			if (leftChild.multiplicativeTreeNegated())
				result = !result;
			if (rightChild.multiplicativeTreeNegated())
				result = !result;
		}
		return result;
	}

	/**
	 * Negates the mathematical expression by applying the unary -.
	 */
	public void negate() {
		negated = !negated;
		log.debug("negating node: " + this);
	}

	/**
	 * Compares this tree with another tree to see whether they are equivalent.
	 * Conditions: both nodes must contain only multiplications and divisions of
	 * attributes and constants.
	 * 
	 * @param other
	 *            The other node to check against.
	 * @return true if the expressions are equivalent, false otherwise
	 */
	private boolean productMatches(MathNode other) {
		if (this.rightOfNegation != other.rightOfNegation)
			return false;
		List<MathNode> l1 = extractTerms();
		List<MathNode> l2 = other.extractTerms();
		for (Iterator<MathNode> it1 = l1.iterator(); it1.hasNext();) {
			MathNode node1 = it1.next();
			boolean foundEqual = false;
			for (Iterator<MathNode> it2 = l2.iterator(); it2.hasNext();) {
				MathNode node2 = it2.next();
				if (node1.value.equals(node2.value)) {
					foundEqual = true;
					break;
				}
			}
			if (!foundEqual)
				return false;
		}
		return true;
	}

	/**
	 * Extracts the free terms in an additive expression.
	 * 
	 * @see MathOptimizer
	 */
	public void reduceAdditive() {
		if (type == Type.LEAF)
			return;
		new MathOptimizer(new AdditiveOptimizer()).reduce();
	}

	/**
	 * Replaces the state hold by the current node with the other state.
	 * 
	 * @param other
	 *            Node where to copy the state from.
	 * @param negate
	 *            Switch sign.
	 */
	private void replace(MathNode other, boolean negate) {
		if (other.leftChild != null) {
			leftChild = new MathNode(other.leftChild);
			rightChild = new MathNode(other.rightChild);
		}
		negated = other.negated;
		if (negate)
			negated = !negated;
		operator = other.operator;
		type = other.type;
		if (other.value != null) {
			value = new Value(other.value);
		}
	}

	/**
	 * Set the quantifier IDs for this tree
	 * 
	 * @param quantifiers
	 *            the hashmap quantifier name - quantifier
	 * @throws QuantifierNotFoundException
	 */
	protected void setQuantifiers(HashMap<String, Quantifier> quantifiers)
			throws QuantifierNotFoundException {
		switch (type) {
		case LEAF:
			value.setQuantifier(quantifiers);
			break;
		case NODE:
			leftChild.setQuantifiers(quantifiers);
			rightChild.setQuantifiers(quantifiers);
		}
	}

	/**
	 * Appends the assignment code for this node into the buffer.
	 * 
	 * @param buf
	 *            The buffer to append to
	 * @param index
	 *            The current node index
	 * @return the next node index
	 */
	public int toContiki(StringBuffer buf, int index) {
		switch (type) {
		case NODE:
			index = leftChild.toContiki(buf, index);
			index = rightChild.toContiki(buf, index);
			buf.append("        { .type = OPERATOR,\n");
			buf.append("          .data.op_code = " + operator.toContiki()
					+ ",\n");
			appendNegated(buf);
			return index + 1;
		case LEAF:
			index = value.toContiki(buf, index);
			appendNegated(buf);
			return index;
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
		switch (type) {
		case LEAF:
			return ((negated) ? "-" : "") + value.toString();
		case NODE:
			return ((negated) ? "-" : "") + "( " + leftChild + " " + operator
					+ " " + rightChild + ")";
		}
		return "invalid node";
	}
}
