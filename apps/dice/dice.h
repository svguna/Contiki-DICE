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

#ifndef __DICE_H
#define __DICE_H
#include "rime.h"

#define LV_ENTRIES 4
#define LV_DROPS 5
#define SIGNATURE_ENTRIES 5
#define LV_CONJS 3

#define MAX_QUANTIFIERS 5
#define MAX_ATTRIBUTES 5
#define MAX_INV_NODES 15
#define MAX_STACK_SIZE 20

// 1s = 128 clock ticks, hence the following is about 23ms
#define TSYNC_ACCURACY 3
#define TSYNC_OVERFLOW 20000
#define TSYNC_MAX 0xffff
#define ts_after_synch(ref, comp) \
    (((comp) > (ref) + TSYNC_ACCURACY && \
       (comp) - (ref) < TSYNC_ACCURACY + TSYNC_OVERFLOW) || \
     ((ref) > (comp) && (ref) - (comp) > TSYNC_MAX - TSYNC_OVERFLOW))
#define ts_after(ref, comp) \
    (((comp) > (ref) && (comp) - (ref) < TSYNC_OVERFLOW)|| \
     ((ref) > (comp) && (ref) - (comp) > TSYNC_MAX - TSYNC_OVERFLOW))
#define ts_after_eq(ref, comp) \
    (((comp) >= (ref) && (comp) - (ref) < TSYNC_OVERFLOW) || \
     ((ref) > (comp) && (ref) - (comp) > TSYNC_MAX - TSYNC_OVERFLOW))


struct view_entry {
    uint16_t val;
    uint16_t attr;
    clock_time_t ts;
    rimeaddr_t src;
};
typedef struct view_entry view_entry_t;


struct view_drop {
    clock_time_t ts;
    rimeaddr_t src;
};
typedef struct view_drop view_drop_t;


struct view_conj {
    uint8_t flagged[MAX_QUANTIFIERS];
    rimeaddr_t src[MAX_QUANTIFIERS];
    clock_time_t ts[MAX_QUANTIFIERS];
};
typedef struct view_conj view_conj_t;

struct dice_view {
    view_entry_t entries[LV_ENTRIES];
    view_drop_t drops[LV_DROPS];
};
typedef struct dice_view dice_view_t;

struct dice_view_t1 {
    view_conj_t conjs[LV_CONJS];
    view_drop_t drops[LV_DROPS];
};
typedef struct dice_view_t1 dice_view_t1_t;




enum {
    OBJ_MAXIMIZE,
    OBJ_MINIMIZE
};

enum {
    BOOL,
    OPERATOR,
    INT,
    ATTRIBUTE
};

struct signature_entry {
    uint16_t attr;
    uint8_t objective;
    uint8_t slice_size;
};
typedef struct signature_entry signature_entry_t;


struct signature {
    uint16_t entries_no;
    signature_entry_t entries[SIGNATURE_ENTRIES];
};
typedef struct signature signature_t;

extern signature_t signature;

void print_entry(char *buf, view_entry_t *entry);
void print_entry_msg(char *msg, view_entry_t *entry);
void print_drop(char *buf, view_drop_t *drop);
void print_view(char *buf, dice_view_t *view);
void print_entries_msg(char *msg, view_entry_t entries[LV_ENTRIES]);
void print_view_msg(char *msg, dice_view_t *view);
void print_viewt1_msg(char *msg, dice_view_t1_t *view);
void print_conjs_msg(char *msg, view_conj_t *conjs);


#endif
