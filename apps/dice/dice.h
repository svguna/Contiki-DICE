#ifndef __DICE_H
#define __DICE_H
#include "rime.h"

#define LV_ENTRIES 2
#define LV_DROPS 5

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
    clock_time_t ts;
    rimeaddr_t src;
};
typedef struct view_entry view_entry_t;


struct view_drop {
    clock_time_t ts;
    rimeaddr_t src;
};
typedef struct view_drop view_drop_t;


struct dice_view {
    view_entry_t entries[LV_ENTRIES];
    view_drop_t drops[LV_DROPS];
};
typedef struct dice_view dice_view_t;

void print_entry(char *buf, view_entry_t *entry);
void print_entry_msg(char *msg, view_entry_t *entry);
void print_drop(char *buf, view_drop_t *drop);
void print_view(char *buf, dice_view_t *view);
void print_entries_msg(char *msg, view_entry_t entries[LV_ENTRIES]);
void print_view_msg(char *msg, dice_view_t *view);


#endif
