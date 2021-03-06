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

#include "dice.h"
#include "history.h"
#include "view_manager.h"
#include "invariant.h"

enum {
    HE_ENTRY,
    HE_DROP
};

struct history_entry {
    uint8_t type;
    union {
        view_entry_t entry;
        view_drop_t drop;
    } data;
};
typedef struct history_entry history_entry_t;


static history_entry_t history[HISTORY_SIZE];
static int history_size;
clock_time_t last_entry = 0;


static void drop_entry(int idx)
{
    int i;

    if (idx < 0 || idx >= history_size)
        return;
    for (i = idx; i < history_size - 1; i++)
        memcpy(history + i, history + i + 1, sizeof(history_entry_t));
    history_size--;
}


static void flush_overflow()
{
    int i = 0;
    clock_time_t now = clock_time();
    
    if (now >= last_entry) {
        last_entry = now;
        return;
    }
    last_entry = now;
    
    while (i < history_size) {
        clock_time_t tts;
        
        switch (history[i].type) {
            case HE_ENTRY:
                tts = history[i].data.entry.ts;
                break;
            case HE_DROP:
                tts = history[i].data.drop.ts;
                break;
            default:
                i++;
                continue;
        }
        if ((now >= tts && now - tts < TSYNC_OVERFLOW) ||
                (tts > now && tts - now > TSYNC_MAX - TSYNC_OVERFLOW)) {
            i++;
            continue;
        }
        drop_entry(i);
    }
}


static int find_position(clock_time_t ts)
{
    int i, oldest = -1;
    clock_time_t oldest_ts = 0xffff, now = clock_time();

    flush_overflow();

    if (ts > now && ts - now < TSYNC_MAX - TSYNC_OVERFLOW) {
        printf("history too old ts=%u, now=%u\n", ts, now);
        return -1;
    }

    if (history_size < HISTORY_SIZE) {
        history_size++;
        return history_size - 1;
    }
    
    for (i = 0; i < history_size; i++) {
        clock_time_t tts;

        switch (history[i].type) {
            case HE_ENTRY:
                tts = history[i].data.entry.ts;
                break;
            case HE_DROP:
                tts = history[i].data.drop.ts;
                break;
            default:
                continue;
        }

        // Here we don't need to adjust for time synch accuracy since doing so
        // would overfill our history buffer.
        if (ts_after_eq(tts, ts) && 
                (oldest == -1 || ts_after(tts, oldest_ts))) {
            oldest = i;
            oldest_ts = tts;
        }
    }
    return oldest;
}


static int drop_exists(rimeaddr_t *addr, clock_time_t from, clock_time_t to)
{
    int i;

    for (i = 0; i < history_size; i++) {
        if (history[i].type != HE_DROP || 
                !rimeaddr_cmp(addr, &history[i].data.drop.src))
            continue;
        // TODO should we adjust for time synch accuracy?
        if (ts_after(from, history[i].data.drop.ts) && 
                ts_after(history[i].data.drop.ts, to)) 
            return 1;
    }
    return 0;
}


static void build_view(clock_time_t ts)
{
    int i;
    view_entry_t entries[LV_ENTRIES];
    inv_node_t result;

    memset(&entries, 0, sizeof(view_entry_t) * LV_ENTRIES);

    for (i = 0; i < history_size; i++) {
        view_entry_t *entry = &history[i].data.entry;

        // TODO should we adjust for time synch accuracy?
        if (history[i].type != HE_ENTRY || ts_after(ts, entry->ts) ||
                drop_exists(&entry->src, entry->ts, ts)) 
            continue;
        push_to_all_slices(entry, entries);
    }

    print_entries_msg("history view ",  entries);
    if (evaluate(entries, &result)) {
        printf("no eval!!!\n");
        return;
    }
    if (result.data.value)
        printf("invariant complied with!\n");
    else
        printf("invariant violated\n");
}


void history_push_entry(view_entry_t *entry)
{
    int idx = find_position(entry->ts);

    if (idx == -1) 
        return;
    
    history[idx].type = HE_ENTRY;
    memcpy(&history[idx].data.entry, entry, sizeof(view_entry_t));

    build_view(entry->ts);
}


void history_push_drop(view_drop_t *drop)
{
    int idx = find_position(drop->ts);

    if (idx == -1) 
        return;
    
    history[idx].type = HE_DROP;
    memcpy(&history[idx].data.drop, drop, sizeof(view_drop_t));

    build_view(drop->ts);
}

