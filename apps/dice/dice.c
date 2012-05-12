#include <stdio.h>
#include "contiki.h"

#include "dice.h"
#include "drickle.h"
#include "attributes.h"
#include "view_manager.h"
#include "group.h"

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


void print_view_msg(char *msg, dice_view_t *view)
{
    char buf[196];
    memcpy(buf, msg, strlen(msg));
    print_view(buf + strlen(msg), view);
    printf("%s\n", buf);
}



void print_entries_msg(char *msg, view_entry_t entries[LV_ENTRIES])
{
    char buf[196];
    memcpy(buf, msg, strlen(msg));
    print_entries(buf + strlen(msg), entries);
    printf("%s\n", buf);
}

