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
#include "contiki.h"

#include "dice.h"
#include "drickle.h"
#include "attributes.h"
#include "view_manager.h"
#include "group.h"
#include "evaluation_manager.h"

PROCESS(dice_main_process, "dice main process");
AUTOSTART_PROCESSES(&dice_main_process);


PROCESS_THREAD(dice_main_process, ev, data)
{
    PROCESS_BEGIN();
    view_manager_init();
    groupmon_init(5 * CLOCK_SECOND, 30 * CLOCK_SECOND);
    drickle_init();
    attributes_init();
    printf("Hello, DiCE world\n");
    PROCESS_END();
}


void print_entry(char *buf, view_entry_t *entry)
{
    if (entry->ts == 0) {
        sprintf(buf, "-");
        return;
    }
    sprintf(buf, "%d@%d.%d(%u)", entry->val, entry->src.u8[1], 
            entry->src.u8[0], entry->ts);
}


void print_conj(char *buf, view_conj_t *conj)
{
    int i;
    for (i = 0; i < MAX_QUANTIFIERS; i++) {
        if (conj->ts[i] == 0) 
            sprintf(buf, "[-]");
        else
            sprintf(buf, "[%d.%d(%u)]", conj->src[i].u8[1], conj->src[i].u8[0], 
                    conj->ts[i]);
        buf = buf + strlen(buf);
    }
}


void print_entry_msg(char *msg, view_entry_t *entry)
{
    char buf[196];
    memcpy(buf, msg, strlen(msg));
    print_entry(buf + strlen(msg), entry);
    printf("%s\n", buf);
}


void print_drop(char *buf, view_drop_t *drop)
{
    if (drop->ts == 0) {
        sprintf(buf, "-");
        return;
    }
    sprintf(buf, "D@%d.%d(%u)", drop->src.u8[1], drop->src.u8[0], drop->ts);
}


static void print_entries(char *buf, view_entry_t entries[LV_ENTRIES])
{
    int i;

    sprintf(buf, "<");
    buf = buf + strlen(buf);
    for (i = 0; i < LV_ENTRIES; i++) {
        print_entry(buf, entries + i);
        buf = buf + strlen(buf);
        if (i < LV_ENTRIES - 1) {
            sprintf(buf, ",");
            buf = buf + strlen(buf);
        }
    }
    sprintf(buf, ">");
}


static void print_conjs(char *buf, view_conj_t conjs[LV_CONJS])
{
    int i;

    sprintf(buf, "<");
    buf = buf + strlen(buf);
    for (i = 0; i < disjunctions_no; i++) {
        print_conj(buf, conjs + i);
        buf = buf + strlen(buf);
        if (i < disjunctions_no - 1) {
            sprintf(buf, ",");
            buf = buf + strlen(buf);
        }
    }
    sprintf(buf, ">");
}


void print_view(char *buf, dice_view_t *view)
{
    int i;
   
    print_entries(buf, view->entries);
    buf = buf + strlen(buf);
    sprintf(buf, "[");
    buf = buf + strlen(buf);
    for (i = 0; i < LV_DROPS; i++) {
        print_drop(buf, view->drops + i);
        buf = buf + strlen(buf);
        if (i < LV_DROPS - 1) {
            sprintf(buf, ",");
            buf = buf + strlen(buf);
        }
    }
    sprintf(buf, "]");
}


void print_viewt1(char *buf, dice_view_t1_t *view)
{
    int i;
   
    print_conjs(buf, view->conjs);
    buf = buf + strlen(buf);
    sprintf(buf, "[");
    buf = buf + strlen(buf);
    for (i = 0; i < LV_DROPS; i++) {
        print_drop(buf, view->drops + i);
        buf = buf + strlen(buf);
        if (i < LV_DROPS - 1) {
            sprintf(buf, ",");
            buf = buf + strlen(buf);
        }
    }
    sprintf(buf, "]");
}


void print_conjs_msg(char *msg, view_conj_t conjs[LV_CONJS])
{
    char data[196];
    memcpy(data, msg, strlen(msg));
   
    print_conjs(data + strlen(msg), conjs);
    printf("%s\n", data);
}


void print_view_msg(char *msg, dice_view_t *view)
{
    char buf[196];
    memcpy(buf, msg, strlen(msg));
    print_view(buf + strlen(msg), view);
    printf("%s\n", buf);
}


void print_viewt1_msg(char *msg, dice_view_t1_t *view)
{
    char buf[196];
    memcpy(buf, msg, strlen(msg));
    print_viewt1(buf + strlen(msg), view);
    printf("%s\n", buf);
}


void print_entries_msg(char *msg, view_entry_t entries[LV_ENTRIES])
{
    char buf[196];
    memcpy(buf, msg, strlen(msg));
    print_entries(buf + strlen(msg), entries);
    printf("%s\n", buf);
}

