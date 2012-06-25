#include <stdio.h>
#include "contiki.h"
#include "net/rime.h"
#include "lib/random.h"

#include "view_manager.h"
#include "dice.h"
#include "attributes.h"
#include "drickle.h"

static struct ctimer att_timer;
uint16_t attribute_value = 0;
int local_attribute_no = 4;
uint16_t local_attribute_hashes[] = {1, 10, 11, 12};
int attribute_initialized = 0;


static void attributes_refresh(void *data)
{
    view_entry_t entry;
    int updated = 0;

    attribute_initialized = 1;
    entry.ts = clock_time();
    entry.val = attribute_value = random_rand() % 100;
    entry.attr = 1;
    memcpy(&entry.src, &rimeaddr_node_addr, sizeof(rimeaddr_t));
    
    if (local_disjunctions_refresh()) {
        updated = 1;
        print_viewt1_msg("T1 ar", &local_view_t1);
    }
    print_entry_msg("attribute refresh ", &entry);
    if (push_entry(&entry)) { 
        print_view_msg("after refresh ", &local_view);
        updated = 1;
    }
    if (updated)
        drickle_reset();
    ctimer_reset(&att_timer);
}


int get_attribute(uint16_t hash, uint16_t *value)
{
    static int r = 0;
    if (hash > 9)
        *value = hash + 1;
    else
        *value = attribute_value;
    if (rimeaddr_node_addr.u8[0] == 2) {
        r++;
        *value = 2;
        if (r > 20)
           *value = 30;
    }
    return 1;
}


void attributes_init()
{
    clock_time_t period = ATTRIBUTE_REFRESH * CLOCK_SECOND;
    ctimer_set(&att_timer, period, &attributes_refresh, NULL);
}

