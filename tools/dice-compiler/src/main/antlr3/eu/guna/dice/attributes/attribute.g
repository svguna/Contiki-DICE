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
	
