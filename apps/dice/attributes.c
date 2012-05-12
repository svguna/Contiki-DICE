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
int attribute_initialized = 0;


static void attributes_refresh(void *data)
{
    view_entry_t entry;

    attribute_initialized = 1;
    entry.ts = clock_time();
    entry.val = attribute_value = random_rand() % 100;
    entry.attr = 1;
    memcpy(&entry.src, &rimeaddr_node_addr, sizeof(rimeaddr_t));
    
    print_entry_msg("attribute refresh ", &entry);
    if (push_entry(&entry)) {
        print_view_msg("after refresh ", &local_view);
        drickle_reset();
    }
    ctimer_reset(&att_timer);
}


void attributes_init()
{
    clock_time_t period = ATTRIBUTE_REFRESH * CLOCK_SECOND;
    ctimer_set(&att_timer, period, &attributes_refresh, NULL);
}

