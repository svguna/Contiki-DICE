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
