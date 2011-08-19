#include <stdio.h>
#include "contiki.h"
#include "net/rime.h"
#include "sys/pt.h"
#include "lib/random.h"

#include "dice.h"
#include "drickle.h"
#include "view_manager.h"
#include "group.h"

static void breceive(struct broadcast_conn *c, rimeaddr_t *from);
static void bsend(void *data);


static struct broadcast_callbacks bcalls = {breceive};
static struct broadcast_conn bconn;
static struct ctimer bcast_timer;
static int initialized = 0;
static int redundant_cnt = 0;
static clock_time_t last_bcast = 0;

clock_time_t tau;

struct dice_pkt {
    clock_time_t timestamp;
    dice_view_t view;
};
typedef struct dice_pkt drickle_pkt_t;


void drickle_reset()
{
    clock_time_t t;
    
    redundant_cnt = 0;
    tau = TRICKLE_LOW;
    t = tau / 2 + (random_rand() % (tau / 2));
    if (initialized && !ctimer_expired(&bcast_timer) &&
            etimer_expiration_time(&bcast_timer.etimer) < clock_time() + t)
        return;
    ctimer_set(&bcast_timer, t, &bsend, NULL);
}


static void increase_timer()
{
    clock_time_t t;
    
    tau *= 2;
    if (tau > TRICKLE_HIGH)
        tau = TRICKLE_HIGH;
    printf("tau is %u\n", tau);
    t = tau / 2 + (random_rand() % (tau / 2));
    ctimer_set(&bcast_timer, t, &bsend, NULL);
}


static void bsend(void *data)
{
    drickle_pkt_t pkt;
    clock_time_t now = clock_time();

    if (now < last_bcast && prune_view(now)) {
        redundant_cnt = 0;
        tau = TRICKLE_LOW;
        printf("prune force reset\n");
        return;
    }

    last_bcast = now;
    increase_timer();
    if (redundant_cnt >= TRICKLE_REDUNDANCY) {
        printf("skip redundancy\n");
        redundant_cnt = 0;
        return;
    }
    
    pkt.timestamp = clock_time();
    memcpy(&pkt.view, &local_view, sizeof(dice_view_t));

    packetbuf_copyfrom(&pkt, sizeof(drickle_pkt_t));
    broadcast_send(&bconn);
    printf("view sent\n");
}


static void update_timestamps(drickle_pkt_t *pkt)
{
    clock_time_t now = clock_time();
    clock_time_t delta;
    int i;

    if (now > pkt->timestamp) {
        delta = now - pkt->timestamp;
        for (i = 0; i < LV_ENTRIES; i++) {
            if (pkt->view.entries[i].ts == 0)
                continue;
            pkt->view.entries[i].ts += delta;
            if (pkt->view.entries[i].ts > now)
                pkt->view.entries[i].ts = now;
        }
        for (i = 0; i < LV_DROPS; i++) {
            if (pkt->view.drops[i].ts == 0)
                continue;
            pkt->view.drops[i].ts += delta;
            if (pkt->view.drops[i].ts > now)
                pkt->view.drops[i].ts = now;
        }
        return;
    }

    delta = pkt->timestamp - now;
    for (i = 0; i < LV_ENTRIES; i++) {
        if (pkt->view.entries[i].ts == 0)
            continue;
        pkt->view.entries[i].ts -= delta;
    }
    for (i = 0; i < LV_DROPS; i++) {
        if (pkt->view.drops[i].ts == 0)
            continue;
        pkt->view.drops[i].ts -= delta;
    }
}


static void breceive(struct broadcast_conn *c, rimeaddr_t *from)
{
    drickle_pkt_t *pkt = (drickle_pkt_t *) packetbuf_dataptr();
    
    printf("received view %d.%d\n", from->u8[1], from->u8[0]);
    if (!groupmon_isalive(from))
        groupmon_forceupdate(from);
    update_timestamps(pkt);
    if (!merge_view(&pkt->view))
        redundant_cnt++;
}


void drickle_init()
{
    broadcast_open(&bconn, DRICKLE_CHANNEL, &bcalls);
    drickle_reset();
    initialized = 1;
}

