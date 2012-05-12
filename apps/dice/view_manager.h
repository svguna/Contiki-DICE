#ifndef __VIEW_MANAGER_H
#define __VIEW_MANAGER_H
#include "dice.h"

extern dice_view_t local_view;

int push_to_all_slices(view_entry_t *entry, view_entry_t entries[LV_ENTRIES]);
int push_entry(view_entry_t *entry);
int merge_view(dice_view_t *other);
int prune_view(clock_time_t ts);

void view_manager_init();

#endif
