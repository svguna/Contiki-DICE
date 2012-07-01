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
#include "net/rime.h"

#include "group.h"


struct group_member {
    rimeaddr_t addr;
    uint32_t clock;
};
typedef struct group_member group_member_t;

struct vc_entry {
    rimeaddr_t addr;
    uint8_t clock_offset;
};
typedef struct vc_entry vc_entry_t;

struct vector_clock {
    uint32_t clock;
    struct vc_entry entries[0];
};
typedef struct vector_clock vector_clock_t;

static group_member_t group[MAX_NODES];
static int group_size = 0;

static uint32_t local_clock;
static uint32_t evict_threshold;

static int broadcast_ticks, broadcast_ticks_cnt;
static int started = 0;

static void receive_vectorclock(struct broadcast_conn *c, 
        const rimeaddr_t *src);
static struct broadcast_callbacks gcalls = {receive_vectorclock};
static struct broadcast_conn gconn;
static struct ctimer clocktick_timer;


static void update_member(group_member_t *member)
{
    int i;

    for (i = 0; i < group_size; i++) {
        if (!rimeaddr_cmp(&group[i].addr, &member->addr))
            continue;
        if (group[i].clock >= member->clock)
            return;
        memcpy(group + i, member, sizeof(group_member_t));
        printf("vc update member %d.%d\n", member->addr.u8[1], 
                member->addr.u8[0]);
        return;
    }

    if (group_size == MAX_NODES) {
        printf("vc too many\n");
        return;
    }

    memcpy(group + group_size, member, sizeof(group_member_t));
    group_size++;
    printf("vc new member %d.%d\n", member->addr.u8[1], member->addr.u8[0]);
}


static void check_all_expired()
{
    int i = 0, j;

    while (i < group_size)
    {
        //+1 because local_clock can be smaller than bigger clock of 1 unit
        if (local_clock + 1 - group[i].clock < evict_threshold) {
            i++;
            continue;
        }

        groupmon_evict(&group[i].addr);
        printf("vc dead member %d.%d\n", group[i].addr.u8[1], 
                group[i].addr.u8[0]);
        for (j = i + 1; j < group_size; j++)
            memcpy(group + j - 1, group + j, sizeof(group_member_t));
        group_size--;
    }
}


static void receive_vectorclock(struct broadcast_conn *c, const rimeaddr_t *src)
{
    int i;
    int need_check = 0;
    group_member_t member;
    vector_clock_t *vc = (vector_clock_t *) packetbuf_dataptr();
    int entry_cnt = 
        (packetbuf_datalen() - sizeof(vector_clock_t)) / sizeof(vc_entry_t);

    printf("vc receive from %d.%d\n", src->u8[1], src->u8[0]);

    if (vc->clock > local_clock + 1) {
        need_check = 1;
        local_clock = vc->clock;
        printf("vc clock sync %u\n", local_clock);
    }

    memcpy(&member.addr, src, sizeof(rimeaddr_t));
    member.clock = vc->clock;
    update_member(&member);

    for (i = 0; i < entry_cnt; i++) {
        uint32_t remote_clock;
        if (rimeaddr_cmp(&vc->entries[i].addr, &rimeaddr_node_addr))
            continue;
        remote_clock = vc->clock + 1 - vc->entries[i].clock_offset;

        memcpy(&member.addr, &vc->entries[i].addr, sizeof(rimeaddr_t));
        member.clock = remote_clock;
        update_member(&member);

        if (remote_clock <= local_clock + 1)
            continue;
        need_check = 1;
        local_clock = remote_clock;
    }

    if (need_check)
        check_all_expired();
}


static void broadcast_vectorclock()
{
    int i;
    uint16_t len;
    char buffer[sizeof(vector_clock_t) + MAX_NODES * sizeof(vc_entry_t)];
    vector_clock_t *vc = (vector_clock_t *) buffer;
    
    vc->clock = local_clock;
    for (i = 0; i < group_size; i++) {
        memcpy(&vc->entries[i].addr, &group[i].addr, sizeof(rimeaddr_t));
        // Assuming that my clock is bigger than anybody else's clock.
        vc->entries[i].clock_offset = local_clock +1 - group[i].clock;
    }

    len = sizeof(vector_clock_t) + group_size * sizeof(struct vc_entry);

    printf("vc broadcast\n");
    packetbuf_copyfrom(buffer, len);
    broadcast_send(&gconn);
}


static void clock_tick(void *data)
{
    local_clock++;
    broadcast_ticks_cnt++;
    check_all_expired();
    if (broadcast_ticks_cnt == broadcast_ticks) {
        broadcast_ticks_cnt = 0;
        broadcast_vectorclock();
    }
    ctimer_reset(&clocktick_timer);
}


int groupmon_init(uint32_t newNeighborLatency, uint32_t missingNeighborLatency)
{
    if (started)
        return -1;

    evict_threshold = missingNeighborLatency / CLOCK_TICK_PERIOD;
    broadcast_ticks = newNeighborLatency / CLOCK_TICK_PERIOD;
    broadcast_ticks_cnt = 0;
    local_clock = evict_threshold;
    group_size = 0;

    broadcast_open(&gconn, GROUP_CHANNEL, &gcalls);
    ctimer_set(&clocktick_timer, CLOCK_TICK_PERIOD, &clock_tick, NULL);
    started = 1;
    return 0;
}


int groupmon_stop()
{
    started = 0;
    ctimer_stop(&clocktick_timer);
    return 0;
}


int groupmon_reset()
{
    local_clock = evict_threshold;
    group_size = 0;
    return 0;
}


void groupmon_forceupdate(rimeaddr_t *addr)
{
    group_member_t member;
   
    memcpy(&member.addr, addr, sizeof(rimeaddr_t));
    member.clock = local_clock;
    update_member(&member);
}


int groupmon_isalive(rimeaddr_t *addr)
{
    int i;

    if (rimeaddr_cmp(addr, &rimeaddr_node_addr))
        return 1;

    for (i = 0; i < group_size; i++) 
        if (rimeaddr_cmp(&group[i].addr, addr))
            return 1;
    return 0;
}

