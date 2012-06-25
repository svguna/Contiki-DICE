#include <stdio.h>
#include "contiki.h"
#include "net/rime.h"
#include "sys/pt.h"
#include "lib/random.h"

#include "dice.h"
#include "drickle.h"
#include "view_manager.h"
#include "group.h"
#include "evaluation_manager.h"

static void breceive(struct broadcast_conn *c);
static void bsend(void *data);


static struct abc_callbacks bcalls = {breceive};
static struct abc_conn bconn;
static struct ctimer bcast_timer;
static int initialized = 0;
static int redundant_cnt = 0;
static clock_time_t last_bcast = 0;

clock_time_t tau;

struct dice_pkt {
    rimeaddr_t src;
    clock_time_t timestamp;
    uint16_t type;
    union {
        dice_view_t data;
        dice_view_t1_t data_t1;
    } view;
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
    t = tau / 2 + (random_rand() % (tau / 2));
    ctimer_set(&bcast_timer, t, &bsend, NULL);
}


static void bsend_t1()
{
    drickle_pkt_t pkt;
    
    memcpy(&pkt.src, &rimeaddr_node_addr, sizeof(rimeaddr_t));
    pkt.timestamp = clock_time();
    pkt.type = 1;
    memcpy(&pkt.view, &local_view_t1, sizeof(dice_view_t1_t));
    packetbuf_copyfrom(&pkt, sizeof(drickle_pkt_t));
    print_viewt1_msg("send view 1", &pkt.view.data_t1);
    abc_send(&bconn);
}


static void bsend_t2()
{
    drickle_pkt_t pkt;
    
    memcpy(&pkt.src, &rimeaddr_node_addr, sizeof(rimeaddr_t));
    pkt.timestamp = clock_time();
    pkt.type = 2;
    memcpy(&pkt.view, &local_view, sizeof(dice_view_t));
    packetbuf_copyfrom(&pkt, sizeof(drickle_pkt_t));
    print_view_msg("send view 2", &pkt.view.data);
    abc_send(&bconn);
}


static void bsend(void *data)
{
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

    bsend_t1();
//    bsend_t2();
}


static void update_timestamps_t1(drickle_pkt_t *pkt)
{
    clock_time_t now = clock_time();
    clock_time_t delta;
    int i, j;

    if (now > pkt->timestamp) {
        delta = now - pkt->timestamp;
        for (i = 0; i < disjunctions_no; i++) {
            for (j = 0; j < MAX_QUANTIFIERS; j++) { 
                if (pkt->view.data_t1.conjs[i].ts[j] == 0)
                    continue;
                pkt->view.data_t1.conjs[i].ts[j] += delta;
                if (pkt->view.data_t1.conjs[i].ts[j] > now)
                    pkt->view.data_t1.conjs[i].ts[j] = now;
            }
        }
        for (i = 0; i < LV_DROPS; i++) {
            if (pkt->view.data_t1.drops[i].ts == 0)
                continue;
            pkt->view.data_t1.drops[i].ts += delta;
            if (pkt->view.data_t1.drops[i].ts > now)
                pkt->view.data_t1.drops[i].ts = now;
        }
        return;
    }

    delta = pkt->timestamp - now;
    for (i = 0; i < disjunctions_no; i++)
        for (j = 0; j < MAX_QUANTIFIERS; j++) {
            if (pkt->view.data_t1.conjs[i].ts[j] == 0)
                continue;
            pkt->view.data_t1.conjs[i].ts[j] -= delta;
        }
    
    for (i = 0; i < LV_DROPS; i++) {
        if (pkt->view.data_t1.drops[i].ts == 0)
            continue;
        pkt->view.data_t1.drops[i].ts -= delta;
    }
}


static void update_timestamps(drickle_pkt_t *pkt)
{
    clock_time_t now = clock_time();
    clock_time_t delta;
    int i;

    if (now > pkt->timestamp) {
        delta = now - pkt->timestamp;
        for (i = 0; i < LV_ENTRIES; i++) {
            if (pkt->view.data.entries[i].ts == 0)
                continue;
            pkt->view.data.entries[i].ts += delta;
            if (pkt->view.data.entries[i].ts > now)
                pkt->view.data.entries[i].ts = now;
        }
        for (i = 0; i < LV_DROPS; i++) {
            if (pkt->view.data.drops[i].ts == 0)
                continue;
            pkt->view.data.drops[i].ts += delta;
            if (pkt->view.data.drops[i].ts > now)
                pkt->view.data.drops[i].ts = now;
        }
        return;
    }

    delta = pkt->timestamp - now;
    for (i = 0; i < LV_ENTRIES; i++) {
        if (pkt->view.data.entries[i].ts == 0)
            continue;
        pkt->view.data.entries[i].ts -= delta;
    }
    for (i = 0; i < LV_DROPS; i++) {
        if (pkt->view.data.drops[i].ts == 0)
            continue;
        pkt->view.data.drops[i].ts -= delta;
    }
}


static void breceive(struct broadcast_conn *c)
{
    drickle_pkt_t pkt;
    rimeaddr_t *from;
    
    memcpy(&pkt, packetbuf_dataptr(), sizeof(drickle_pkt_t));;
    from = &pkt.src;
   
    if (pkt.type == 2)
        print_view_msg("received view ", &pkt.view.data);
    else
        print_viewt1_msg("rt1", &pkt.view.data_t1);
    printf("received view %d.%d\n", from->u8[1], from->u8[0]);
    if (!groupmon_isalive(from))
        groupmon_forceupdate(from);
    if (pkt.type == 1)
        update_timestamps_t1(&pkt);
    else    
        update_timestamps(&pkt);
    
    if ((pkt.type == 1 && merge_disjunctions(&pkt.view.data_t1))
            || (pkt.type == 2 && merge_view(&pkt.view.data)))
        drickle_reset();
    else
        // TODO two packets = 1 view
        redundant_cnt++;
}


void drickle_init()
{
    abc_open(&bconn, DRICKLE_CHANNEL, &bcalls);
    drickle_reset();
    initialized = 1;
}

