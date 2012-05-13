#ifndef __INVARIANT_H
#define __INVARIANT_H

#include "dice.h"

enum {
  BOOL_AND = 0,
  BOOL_IMPLY = 1,
  BOOL_OR = 2,
  COMP_DIFFERENT = 3,
  COMP_EQUAL = 4,
  COMP_GREATER = 5,
  COMP_LOWER = 6,
  /* Operators used only by math nodes follow.
   * IMPORTANT! Do not add any boolean / comparison operator after this line.
   */
  MATH_DIV = 7,
  MATH_MINUS = 8,
  MATH_MOD = 9,
  MATH_MUL = 10,
  MATH_PLUS = 11
};

struct attribute {
    uint16_t hash;
    uint16_t quantifier;
};
typedef struct attribute attribute_t;


struct inv_node {
    uint8_t type;
    uint8_t negated;
    union {
        int value;
        attribute_t attribute;
        uint8_t op_code;
    } data;
};
typedef struct inv_node inv_node_t;


struct invariant {
    uint8_t quantifiers_no;
    uint8_t quantifiers[MAX_QUANTIFIERS];
    uint8_t nodes_no;
    inv_node_t nodes[MAX_INV_NODES];
};
typedef struct invariant invariant_t;


struct mapping_data {
    uint16_t attribute;
    uint8_t math_id;
    uint8_t quantifier;
    uint8_t index;
};
typedef struct mapping_data mapping_data_t;


struct mapping {
    uint16_t data_no;
    mapping_data_t data[MAX_QUANTIFIERS * MAX_ATTRIBUTES];
};
typedef struct mapping mapping_t;

#endif

