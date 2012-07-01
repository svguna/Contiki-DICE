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

#include <stdio.h>

#include "attributes.h"
#include "invariant.h"

typedef struct stack {
    int size;
    inv_node_t nodes[MAX_STACK_SIZE];
} stack_t;


static int stack_push(stack_t *stack, inv_node_t *to_push)
{
    if (stack->size == MAX_STACK_SIZE)
        return -1;
    memcpy(stack->nodes + stack->size, to_push, sizeof(inv_node_t));
    stack->size++;
    return 0;
}


static int stack_pop(stack_t *stack, inv_node_t *to_pop)
{
    if (stack->size < 1)
        return -1;
    stack->size--;
    memcpy(to_pop, stack->nodes + stack->size, sizeof(inv_node_t));
    return 0;
}


static int evaluate_and(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = v1 && v2;
    printf("%d && %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_or(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = v1 || v2;
    printf("%d || %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_diff(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = v1 != v2;
    printf("%d != %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_equal(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = v1 == v2;
    printf("%d == %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_imply(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = !v1 || (v1 && v2);
    printf("%d -> %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_greater(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = v1 > v2;
    printf("%d > %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_lower(inv_node_t *noder, int v1, int v2)
{
    noder->type = BOOL;
    noder->data.value = v1 < v2;
    printf("%d < %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_minus(inv_node_t *noder, 
        int v1, int v2)
{
    noder->type = INT;
    noder->data.value = v1 - v2;
    printf("%d - %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_plus(inv_node_t *noder, int v1, int v2)
{
    noder->type = INT;
    noder->data.value = v1 + v2;
    printf("%d + %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_mul(inv_node_t *noder, int v1, int v2)
{
    noder->type = INT;
    noder->data.value = v1 * v2;
    printf("%d * %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_div(inv_node_t *noder, int v1, int v2)
{
    noder->type = INT;
    noder->data.value = v1 / v2;
    printf("%d / %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int evaluate_mod(inv_node_t *noder, int v1, int v2)
{
    noder->type = INT;
    noder->data.value = v1 % v2;
    printf("%d mod %d = %d\n", v1, v2, noder->data.value);
    return 0;
}


static int get_index(int math_id, uint16_t attribute, uint8_t quantifier)
{
    int i;
    for (i = 0; i < mapping.data_no; i++)
        if (mapping.data[i].math_id == math_id &&
                mapping.data[i].attribute == attribute &&
                mapping.data[i].quantifier == quantifier) {
            printf("mapping %d %d %d = %d\n", math_id, attribute, quantifier,
                    mapping.data[i].index);
            return mapping.data[i].index;
        }
    return -1;
}


static int (*operators[])(inv_node_t *, int, int) =
        {evaluate_and, evaluate_imply, evaluate_or, evaluate_diff, 
            evaluate_equal, evaluate_greater, evaluate_lower, evaluate_div, 
            evaluate_minus, evaluate_mod, evaluate_mul, evaluate_plus};


static int evaluate_nodes(view_entry_t entries[LV_ENTRIES], inv_node_t *n1,
        inv_node_t *n2, int math_id, uint8_t op_code, inv_node_t *noder)
{
    int v1 = n1->data.value;
    int v2 = n2->data.value;

    if (n1->type == ATTRIBUTE) {
        int idx = get_index(math_id, n1->data.attribute.hash, 
                n1->data.attribute.quantifier);
        if (idx < 0 || idx >= LV_ENTRIES || entries[idx].ts == 0)
            return -1;
        v1 = entries[idx].val;
    }
    if (n2->type == ATTRIBUTE) {
        int idx = get_index(math_id, n2->data.attribute.hash, 
                n2->data.attribute.quantifier);
        if (idx < 0 || idx >= LV_ENTRIES || entries[idx].ts == 0)
            return -1;
        v2 = entries[idx].val;
    }

    noder->negated = 0;

    if (op_code != BOOL_AND && op_code != BOOL_OR && op_code != BOOL_IMPLY) {
        if (n1->negated)
            v1 = -v1;
        if (n2->negated)
            v2 = -v2;
    }
    return operators[op_code](noder, v1, v2);
}


int evaluate(view_entry_t entries[LV_ENTRIES], inv_node_t *noder)
{
    int i;
    stack_t stack;
    inv_node_t n1, n2;
    int math_id = 0;

    stack.size = 0;

    for (i = 0; i < invariant.nodes_no; i++) {
        if (invariant.nodes[i].type != OPERATOR) {
            if (stack_push(&stack, invariant.nodes + i))
                return -1;
            continue;
        }

        if (stack_pop(&stack, &n2))
            return -1;
        if (stack_pop(&stack, &n1))
            return -1;

        if (invariant.nodes[i].data.op_code < COMP_DIFFERENT)
            math_id++;
       
        printf("math id=%d\n", math_id);
        if (evaluate_nodes(entries, &n1, &n2, math_id, 
                    invariant.nodes[i].data.op_code, noder))
            return -1;
        
        stack_push(&stack, noder);
    }
    return 0;
}


static int evaluate_local_t1(inv_node_t *n1, inv_node_t *n2, uint8_t op_code,
        inv_node_t *noder)
{
    int v1 = n1->data.value;
    int v2 = n2->data.value;

    if (n1->type == ATTRIBUTE) {
        uint16_t value;
        if (!get_attribute(n1->data.attribute.hash, &value)) {
            printf("ga %d=%d\n", n1->data.attribute.hash, value);
            noder->type = INT;
            noder->data.value = value;
            return 0;
        }
        v1 = value;
    }

    noder->negated = 0;

    if (op_code != BOOL_AND && op_code != BOOL_OR && op_code != BOOL_IMPLY) {
        if (n1->negated)
            v1 = -v1;
        if (n2->negated)
            v2 = -v2;
    }
    return operators[op_code](noder, v1, v2);
}


static int evaluate_local_conj(invariant_t *disjunction, view_conj_t *view_conj)
{
    int i;
    
    uint8_t violated_quantifiers[MAX_QUANTIFIERS];
    memset(violated_quantifiers, 0, sizeof(violated_quantifiers));

    for (i = 0; i < disjunction->nodes_no; i += 3) {
        inv_node_t noder;
        memset(&noder, 0, sizeof(noder));
        inv_node_t *n1 = disjunction->nodes + i;
        inv_node_t *n2 = disjunction->nodes + i + 2;
        uint8_t op_code = disjunction->nodes[i + 1].data.op_code;
        
        if (evaluate_local_t1(n1, n2, op_code, &noder))
            return -1;

        if (noder.data.value)
            continue;
        printf("vc %d\n", n1->data.attribute.quantifier);
        violated_quantifiers[n1->data.attribute.quantifier] = 1;
    }

    for (i = 0; i < MAX_QUANTIFIERS; i++) {
        if (view_conj->flagged[i] &&
                !rimeaddr_cmp(view_conj->src + i, &rimeaddr_node_addr))
            continue;
        if (view_conj->flagged[i] == violated_quantifiers[i]) 
            continue;
     
        view_conj->flagged[i] = violated_quantifiers[i];
        memcpy(view_conj->src + i, &rimeaddr_node_addr, sizeof(rimeaddr_t));
        view_conj->ts[i] = clock_time();
        printf("vc %d=%d\n", i, view_conj->flagged[i]);
    }

    return 0;
}


void evaluate_local_disjunctions(view_conj_t view_conjs[LV_CONJS])
{
    int i;
    for (i = 0; i < disjunctions_no; i++) 
        evaluate_local_conj(disjunctions + i, view_conjs + i) > 0;
}


int evaluate_disjunctions(view_conj_t view_conjs[LV_CONJS])
{
    int i, j, complied_with = 0;
    
    for (i = 0; i < disjunctions_no; i++) {
        int violated = 0;
        for (j = 0; j < MAX_QUANTIFIERS; j++)
            if (view_conjs[i].flagged[j] && view_conjs[i].ts[j] > 0) {
                violated = 1;
                break;
            }
        if (!violated)
            complied_with = 1;
    }
    if (complied_with)
        printf("T1 complied!\n");
    else
        printf("T1 violated!\n");
    return 0;
}
