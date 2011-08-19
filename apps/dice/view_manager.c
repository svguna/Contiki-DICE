#include <stdio.h>

#include "rimeaddr.h"

#include "dice.h"
#include "view_manager.h"
#include "attributes.h"
#include "drickle.h"
#include "group.h"
#include "history.h"

dice_view_t local_view;


static void shift_left(view_entry_t entries[LV_ENTRIES], int idx)
{
    int i;

    for (i = idx; i < LV_ENTRIES - 1; i++)
        memcpy(entries + i, entries + i + 1, sizeof(view_entry_t));
    entries[LV_ENTRIES - 1].ts = 0;
}


static void shift_right(view_entry_t entries[LV_ENTRIES], int idx)
{
    int i;

    for (i = LV_ENTRIES - 1; i > idx; i--)
        memcpy(entries + i, entries + i - 1, sizeof(view_entry_t));
}


static int push_drop(view_drop_t *drop)
{
    int i, to_insert = -1, oldest_idx = -1;
    clock_time_t oldest_ts = 0;

    for (i = 0; i < LV_DROPS; i++) {
        if (!rimeaddr_cmp(&local_view.drops[i].src, &drop->src))
            continue;
        if (!ts_after_synch(local_view.drops[i].ts, drop->ts))
            return 0;
        local_view.drops[i].ts = drop->ts;
        history_push_drop(drop);
        return 1;
    }

    for (i = 0; i < LV_DROPS; i++) {
        if (local_view.drops[i].ts == 0) {
            to_insert = i;
            break;
        }
        // Here's fine if we don't ajust for time synch accuracy since all we
        // care about is the oldest entry in the history buffer.
        if (oldest_idx == -1 || ts_after(local_view.drops[i].ts, oldest_ts)) {
            oldest_idx = i;
            oldest_ts = local_view.drops[i].ts;
        }
    }

    if (to_insert == -1)
        to_insert = oldest_idx;
    if (to_insert == -1) {
        printf("drop error %d\n", i);
        return 0;
    }

    printf("inserted drop %d\n", to_insert);
    memcpy(local_view.drops + to_insert, drop, sizeof(view_drop_t));
    history_push_drop(drop);
    return 1;
}


static int push_existing(view_entry_t entries[LV_ENTRIES], int idx, 
        view_entry_t *entry)
{
    int cond_ts = (ts_after_synch(entries[idx].ts, entry->ts)) ? 1 : 0;
    int cond_val = (entries[idx].val < entry->val) ? 1 : 0;
    clock_time_t old_ts = entries[idx].ts;

    if (entries[idx].val == entry->val) 
        return 0;
    if (cond_ts) {
        shift_left(entries, idx);
        push_entry_targetted(entries, entry);
    }

    if (entries != local_view.entries)
        return 0;

    if (cond_ts != cond_val) {
        view_drop_t drop;
        if (cond_ts) 
            drop.ts = old_ts;
        else
            drop.ts = entry->ts;
        memcpy(&drop.src, &entry->src, sizeof(rimeaddr_t));
        push_drop(&drop);
    }
    return 1;
}


static int is_obsolete(view_entry_t *entry)
{
    int i;

    for (i = 0; i < LV_DROPS; i++) {
        // TODO What should be the policy for time synch accuracy when flushing
        // values out of the view?
        if (rimeaddr_cmp(&entry->src, &local_view.drops[i].src) &&
                ts_after_eq(entry->ts, local_view.drops[i].ts))
            return 1;
    }
    return 0;
}


int push_entry_targetted(view_entry_t entries[LV_ENTRIES], view_entry_t *entry)
{
    int i;
    for (i = 0; i < LV_ENTRIES; i++) 
        if (entries[i].ts != 0 && rimeaddr_cmp(&entry->src, &entries[i].src)) 
            return push_existing(entries, i, entry);

    for (i = 0; i < LV_ENTRIES; i++) { 
        if (entries[i].ts != 0 && entries[i].val > entry->val)
            continue;
        shift_right(entries, i);
        memcpy(entries + i, entry, sizeof(view_entry_t));
        return 1;
    }
    return 1;
}


/**
 * \return non-zero if the caller should be updated
 */
int push_entry(view_entry_t *entry)
{
    if (is_obsolete(entry) || !groupmon_isalive(&entry->src)) 
        return 1;
    if (!push_entry_targetted(local_view.entries, entry))
        return 0;
    history_push_entry(entry);
    return 1;
}


int prune_obsolete(view_drop_t *drop)
{
    int i;

    for (i = 0; i < LV_ENTRIES; i++) {
        if (local_view.entries[i].ts == 0)
            return 0;
        if (!rimeaddr_cmp(&local_view.entries[i].src, &drop->src))
            continue;
        // TODO what should be the policy for flushing values out of the view?
        if (ts_after(drop->ts, local_view.entries[i].ts))
            return 0;
        shift_left(local_view.entries, i);
        return 1;
    }
    return 0;
}


static int prune_all_obsolete(dice_view_t *other)
{
    int i, need_update = 0;

    for (i = 0; i < LV_DROPS; i++) {
        if (other->drops[i].ts == 0)
            continue;
        if (prune_obsolete(other->drops + i))
            need_update = 1;
    }
    return need_update;
}


static int push_local_value()
{
    view_entry_t entry;

    if (!attribute_initialized)
        return 0;
    entry.ts = clock_time();
    entry.val = attribute_value;
    memcpy(&entry.src, &rimeaddr_node_addr, sizeof(rimeaddr_t));
    return push_entry(&entry);
}


static int compare_entries(view_entry_t entries1[LV_ENTRIES], 
        view_entry_t entries2[LV_ENTRIES])
{
    int i, empty = 0, different_src = 0;

    for (i = 0; i < LV_ENTRIES; i++) {
        if (entries1[i].ts == 0 && entries2[i].ts == 0) {
            empty++;
            continue;
        }
        if (entries1[i].ts != 0 && entries2[i].ts != 0) {
            if (entries1[i].val != entries2[i].val)
                return 0;
            if (!rimeaddr_cmp(&entries1[i].src, &entries2[i].src))
                different_src++;
            continue;
        }
        return 0;
    }
    if (empty > 0 && different_src > 0)
        return 0;
    return 1;
}


int merge_view(dice_view_t *other)
{
    int i, need_update;
    clock_time_t now = clock_time();

    print_view_msg("before merge ", &local_view);
    print_view_msg("merge with ", other);

    need_update = compare_entries(local_view.entries, other->entries) == 0;
    prune_all_obsolete(other);
    for (i = 0; i < LV_DROPS; i++) {
        if (other->drops[i].ts == 0 || 
                rimeaddr_cmp(&other->drops[i].src, &rimeaddr_node_addr) || 
                other->drops[i].ts > now || other->drops[i].src.u8[1] != 0 || 
                other->drops[i].src.u8[1] > MAX_NODES) 
            continue;
        push_drop(other->drops + i);
    }
    if (need_update) {
        for (i = 0; i < LV_ENTRIES; i++) {
            if (other->entries[i].ts == 0 ||
                    rimeaddr_cmp(&other->entries[i].src, &rimeaddr_node_addr) ||
                    other->entries[i].ts > now || 
                    other->entries[i].src.u8[1] != 0 || 
                    other->entries[i].src.u8[0] > MAX_NODES)
                continue;
            push_entry(other->entries + i);
        }
        push_local_value();
        drickle_reset();
    }
    print_view_msg("after merge ", &local_view);
    printf("need update: %d\n", need_update);

    return need_update;
}


void groupmon_evict(rimeaddr_t *addr)
{
    view_drop_t drop;

    memcpy(&drop.src, addr, sizeof(rimeaddr_t));
    drop.ts = clock_time();

    history_push_drop(&drop);
    if (prune_obsolete(&drop))
        drickle_reset();
}


int prune_view(clock_time_t ts)
{
    int i = 0, need_update = 0;

    while (i < LV_ENTRIES) {
        if (local_view.entries[i].ts <= ts) {
            i++; 
            continue;
        }
        shift_left(local_view.entries, i);
        need_update = 1;
    }
    for (i = 0; i < LV_DROPS; i++) {    
        if (local_view.drops[i].ts > ts) { 
            local_view.drops[i].ts = 0;
            need_update = 1;
        }
    }
    if (need_update)
        print_view_msg("after prune", &local_view);
    return need_update;
}
