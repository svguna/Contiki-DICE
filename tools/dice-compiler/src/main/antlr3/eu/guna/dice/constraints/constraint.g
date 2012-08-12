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
grammar constraint;

@header {
package eu.guna.dice.constraints;

import eu.guna.dice.constraints.operators.*;
}

@lexer::header {
package eu.guna.dice.constraints;

import eu.guna.dice.constraints.operators.*;
}

@members {
	ConstraintTable constraintTable = new ConstraintTable();
	
	public ConstraintTable getConstraintTable() {
		return constraintTable;
	}
}

spec_list
	:	spec+
	;
	
spec	:	constraint_spec
	|	constraint_alias
	;
	
constraint_spec
	:	'constraint' '{' bool_expr '}'
	{ constraintTable.addConstraint($bool_expr.node); }
	;

constraint_alias
	:	'constraint' IDENTIFIER '{' bool_expr '}'
	{ constraintTable.addAlias($IDENTIFIER.text, $bool_expr.node); }
	;
	
value returns [Value value]	
	:	attribute
	{ $value = new Value($attribute.att); }
	|	e=NO_INTEGER
	{ $value = new Value(Integer.parseInt($e.text)); }
	|	e=NO_FLOAT
	{ $value = new Value(Float.parseFloat($e.text)); }
	|	'true'
	{ $value = new Value(true); }
	|	'false'
	{ $value = new Value(false); }
	;
	
	
bool_expr returns [BoolNode node]
	:	q1=quantification_spec
	(',' q2=quantification_spec { $q1.quant.merge($q2.quant); } )* ':' e=bool_expr
	{ $node = $e.node; $node.setQuantifications($q1.quant); }
	|	t1=bool_term
	{ $node = $t1.node; }
	(bool_op t2=bool_term { $node = $bool_op.op.joinNodes($node, $t2.node); })*
	|	IDENTIFIER
	{ $node = new BoolNode(constraintTable.getConstraintByAlias($IDENTIFIER.text)); }
	;
		
bool_op returns [BoolOperator op]
	:	'and'
	{ $op = BoolOperator.AND; }
	|	'or'
	{ $op = BoolOperator.OR; }
	|	'->'
	{ $op = BoolOperator.IMPLY; }
	|	'<->'
	{ $op = BoolOperator.DBL_IMPLY; }
	;
	
bool_term returns [BoolNode node]
//	:	('('* '-'? value) => l=expr comparison_op r=expr
	:	l=expr comparison_op r=expr
	{ $node = $comparison_op.op.joinNodes($l.node, $r.node); }
	|	'[' bool_expr ']'
	{ $node = $bool_expr.node; }
	|	'not' '[' bool_expr ']'
	{ $bool_expr.node.negate(); $node = $bool_expr.node; }
	;
	
quantification_spec returns [Quantifications quant]
	:	'any'  i1=IDENTIFIER 
	{ $quant = new Quantifications($i1.text, Quantifier.Type.UNIVERSAL); }
	(',' i2=IDENTIFIER { $quant.add($i2.text, Quantifier.Type.UNIVERSAL); } )*
	|	'exists'  i1=IDENTIFIER 
	{ $quant = new Quantifications($i1.text, Quantifier.Type.EXISTENTIAL); }
	(',' i2=IDENTIFIER { $quant.add($i2.text, Quantifier.Type.EXISTENTIAL); } )*
	;
	
comparison_op returns [ComparisonOperator op]
	:	'<'
	{ $op = ComparisonOperator.LOWER; }
	|	'>'
	{ $op = ComparisonOperator.GREATER; }
	|	'='
	{ $op = ComparisonOperator.EQUAL; }
	|	'!='
	{ $op = ComparisonOperator.DIFFERENT; }
	;

attribute returns [Attribute att]
	:	n=IDENTIFIER '@' q=IDENTIFIER
	{ $att = new Attribute($n.text, $q.text); }
	|	p=IDENTIFIER '.' v=IDENTIFIER '.' f=IDENTIFIER '(' ')' '@' q=IDENTIFIER
	{ $att = new Attribute($p.text, $v.text, $f.text, $q.text); }
	;

expr returns [MathNode node]
	:	t1=math_term {$node = $t1.node; }
	(sum_op t2=math_term {$node = $sum_op.op.joinNodes($node, $t2.node); })*
	;
	
sum_op returns [MathOperator op]
	:	'+'
	{ $op = MathOperator.PLUS; }
	|	'-'
	{ $op = MathOperator.MINUS; }	
	;
	
math_term returns [MathNode node]
	:	t1=math_atom { $node = $t1.node; }
	(mult_op t2=math_atom {$node = $mult_op.op.joinNodes($node, $t2.node); })*
	;
	
mult_op returns [MathOperator op]
	:	'*'
	{ $op = MathOperator.MUL; }
	|	'/'
	{ $op = MathOperator.DIV; }
	|	'%'
	{ $op = MathOperator.MOD; }
	;	
	
math_atom returns [MathNode node]
	: 	value
	{ $node = new MathNode($value.value); }
	|	'(' expr ')'
	{ $node = $expr.node; }
	|	'-' value
	{ $node = new MathNode($value.value); $node.negate(); }
	|	'-' '(' expr ')'
	{ $expr.node.negate(); $node = $expr.node; }
	;
	
	
IDENTIFIER
	:	('a'..'z'|'A'..'Z'|'_')('0'..'9'|'a'..'z'|'A'..'Z'|'_')*
	;
	
NO_INTEGER
	:	('+'|'-')?('0'..'9')+
	;

NO_FLOAT
	:	('+'|'-')?('0'..'9')+('.'('0'..'9')+)?
	;

WS
	: (' '|'\r'|'\t'|'\u000C'|'\n') { $channel=HIDDEN; }
	;
