#ifndef __VIEW_MANAGER_H
#define __VIEW_MANAGER_H
#include "dice.h"

extern dice_view_t local_view;
extern dice_view_t1_t local_view_t1;

int push_to_all_slices(view_entry_t *entry, view_entry_t entries[LV_ENTRIES]);
int push_entry(view_entry_t *entry);
int merge_view(dice_view_t *other);
int merge_disjunctions(dice_view_t1_t *other);
int prune_view(clock_time_t ts);

int local_disjunctions_refresh();
void view_manager_init();

#endif
