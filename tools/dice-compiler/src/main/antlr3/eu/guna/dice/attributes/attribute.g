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
grammar attribute;

@header {
package eu.guna.dice.attributes;

import eu.guna.dice.attributes.exceptions.AttributeAlreadyDefinedException;
import org.antlr.runtime.RecognitionException;
}

@lexer::header {
package eu.guna.dice.attributes;
}

@members {
	private AttributeTable attTable = new AttributeTable();

	public AttributeTable getAttributeTable() {
		return attTable;
	}	
}

spec_list
	:	spec+
	;
	
spec	
	:	e=attribute_spec ';'
	{ attTable.addAttribute($e.att); }
	;
	catch [AttributeAlreadyDefinedException ex] {
		throw new RecognitionException();
	}

attribute_spec returns [Attribute att]
	:	'attribute' basic_type IDENTIFIER '=' value
	{ $att = new Attribute($IDENTIFIER.text, $basic_type.type, new Value($value.text)); }
	|	'attribute' basic_type IDENTIFIER 'on' 'event'
	{ $att = new Attribute($IDENTIFIER.text, $basic_type.type, new Value()); }
	|	'attribute' basic_type IDENTIFIER 'every' per=NO_INTEGER
	{ $att = new Attribute($IDENTIFIER.text, $basic_type.type, new Value(Integer.parseInt($per.text))); }			
	;
	
basic_type returns [Attribute.Type type]
	: 'int' 
	{ $type = Attribute.Type.INT; }
	| 'float'
	{ $type = Attribute.Type.FLOAT; }
	| 'bool'
	{ $type = Attribute.Type.BOOL; }
	;
	
value	
	:	NO_INTEGER
	|	NO_FLOAT
	|	'TRUE'
	|	'FALSE'
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
	
