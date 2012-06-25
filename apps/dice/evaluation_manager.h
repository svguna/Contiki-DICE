#ifndef __EVALUATION_MANAGER_H
#define __EVALUATION_MANAGER_H

#include "dice.h"
#include "invariant.h"

extern int disjunctions_no;
void evaluate_local_disjunctions(view_conj_t view_conjs[LV_CONJS]);
int evaluate(view_entry_t entries[LV_ENTRIES], inv_node_t *noder);

#endif
