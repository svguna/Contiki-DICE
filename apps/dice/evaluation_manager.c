#include <stdio.h>

#include "invariant.h"

invariant_t invariant = {
    .quantifiers_no = 2,
    .quantifiers = { QUANT_UNIVERSAL, QUANT_UNIVERSAL},
    .nodes_no = 7,
    .nodes = {
        { .type = ATTRIBUTE,
          .negated = 0,
          .data.attribute.hash = 1,
          .data.attribute.quantifier = 0
        },
        { .type = ATTRIBUTE,
          .negated = 0,
          .data.attribute.hash = 1,
          .data.attribute.quantifier = 1
        },
        { .type = OPERATOR,
          .negated = 0,
          .data.op_code = MATH_PLUS,
        },
        { .type = INT,
          .negated = 0,
          .data.value = 100,
        },
        { .type = OPERATOR,
          .negated = 0,
          .data.op_code = MATH_MINUS,
        },
        { .type = INT,
          .negated = 0,
          .data.value = 0,
        },
        { .type = OPERATOR,
          .negated = 0,
          .data.op_code = COMP_LOWER,
        },
    }
};
mapping_t mapping = {
    .data_no = 2,
    .data = {
        { .attribute = 1,
          .math_id = 0,
          .quantifier = 0,
          .index = 0
        },
        { .attribute = 1,
          .math_id = 0,
          .quantifier = 1,
          .index = 1
        },
    }
};

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
    // TODO what's math_id?
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

