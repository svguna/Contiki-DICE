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

#ifndef __INVARIANT_H
#define __INVARIANT_H

#include "dice.h"

enum {
    QUANT_UNIVERSAL,
    QUANT_EXISTENTIAL
};

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
    int type;
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


extern invariant_t disjunctions[];
extern int disjunctions_no;
extern invariant_t invariant;
extern mapping_t mapping;

#endif

