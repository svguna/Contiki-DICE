#include "contiki.h"
#include "net/rime/neighbor.h"
#include "net/rime/neighbor-discovery.h"
#include "vector-clock.h"
#include "lib/memb.h"
#include "lib/list.h"
#include "net/rime/runicast.h"
#include "lib/random.h"
#include "dev/watchdog.h"
#include "script-header.h"

#include <stdio.h>
#define DEBUG 0
#if DEBUG
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif

//static int net_size = 0;
//static int num_msg = 0;
//static int  sent_msg = 0;


uint16_t net_ts[MAX_REACHABLE_NEIGHBORS];
uint16_t net_ts_history[MAX_REACHABLE_NEIGHBORS][MAX_REACHABLE_NEIGHBORS];
//uint32_t old_net_ts[MAX_REACHABLE_NEIGHBORS];
uint16_t max_ts = 10;
uint8_t this_id = 0;
uint16_t turn = 0;

static struct ctimer t;
static struct ctimer broadcasttimer;
static struct broadcast_conn bcast;
int max_time = 3;
static long soft_num_msg = 0;
static long soft_size_msg = 0;
static long soft_num_ack = 0;
static long soft_size_ack = 0;
static long soft_num_beacon = 0;
static long soft_size_beacon = 0;
static long recv_size = 0;
static long soft_broadcast_header_size =
  sizeof (struct packetbuf_attr) + sizeof (struct packetbuf_addr);

/*---------------------------------------------------------------------------*/
PROCESS (example_polite_process, "");
AUTOSTART_PROCESSES (&example_polite_process);
/*---------------------------------------------------------------------------*/

int
reach_num ()
{
  int tot = 0;
  int i;
  for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
    {
      if (net_ts[i] + max_time > max_ts)
	{
	  tot++;
	}
    }
  return tot;
}

int
inc_reach_num ()
{
  int tot = 0;
  int i;
  for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
    {
      //count all nodes included in the last complete timestamp array AND offset ids different from 0 
      if (net_ts_history[this_id][i] + max_time >
	  net_ts_history[this_id][this_id])
	{
	  if (net_ts[i] != max_ts)
	    tot++;
	}
      else
	{
	  //add also timestamps of nodes that where not in the last timestamp array and are alive (new nodes from the last TA)
	  if (net_ts[i] + max_time > max_ts)
	    tot++;
	}
    }
  return tot;
}

void
set_ts (uint16_t ts, uint8_t addr)
{

  if (ts > net_ts[addr])
    net_ts[addr] = ts;
  if (ts > max_ts)
    max_ts = ts;
}


uint16_t
get_ts (uint8_t addr)
{
  return net_ts[addr];
}

static void
periodic (void *ptr)
{
  max_ts++;
  net_ts[this_id] = max_ts;
  ctimer_set (&t, CLOCK_SECOND * 40, periodic, NULL);
}

static void
adv_received (struct neighbor_discovery_conn *c, const rimeaddr_t * from,
	      uint16_t val)
{
}


static void
node_rm (rimeaddr_t n, struct neighbor *nn)
{
}

static void
recv_bcast (struct broadcast_conn *broadcast, const rimeaddr_t * from)
{
  uint8_t *size, *type;
  ts_msg *ts;
  uint16_t *main_ts, m_ts;
  uint8_t i, sz, tp, sender_id;

  sender_id = ((*from).u8[0]) - 1;
  //get pointer to the packet header
  main_ts = packetbuf_dataptr ();
  type = (uint8_t *) (main_ts + 1);
  size = (uint8_t *) (type + 1);
  ts = (ts_msg *) (size + 1);
  //get fixed header fields
  m_ts = *main_ts;
  sz = *size;
  tp = *type;
//update statistics
  recv_size +=
    sizeof (ts_msg) * (sz) + sizeof (uint16_t) + sizeof (uint8_t) * 2 +
    soft_broadcast_header_size;
  set_ts (m_ts, sender_id);
//save received timestamp in history
  net_ts_history[sender_id][sender_id] = m_ts;
// it is a full timestamp array
  if (tp == 1)
    {
	      //PRINTF ("TAfrom(%d)[%d]:[",sender_id,sz);
//get timestamp stored in the packet
      for (i = 0; i < sz; i++)
	{
          //PRINTF ("%d:%d,", ts[i].addr, m_ts - ts[i].timestamp);
	  set_ts (m_ts - ts[i].timestamp, ts[i].addr);
//save received timestamp array in history
	  net_ts_history[sender_id][ts[i].addr] = m_ts - ts[i].timestamp;
	}
	     // PRINTF ("]\n");
    }
// it is an update array
  if (tp == 2)
    {
      for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
	{
	  //update the timestamp of all nodes in the last TA as they have an offset of 0 
	  if (net_ts_history[sender_id][i] + max_time >
	      net_ts_history[sender_id][sender_id])
	    {
	      net_ts_history[sender_id][i] = m_ts;
	    }
	}
      for (i = 0; i < sz; i++)
	{
//save received timestamp array in history
	  net_ts_history[sender_id][ts[i].addr] = m_ts - ts[i].timestamp;
	}
//now update my TA with the fresh values received from sender_id
      for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
	{
	  set_ts (net_ts_history[sender_id][i], i);
	}
    }

}

static void
send_TA ()
{
  /*
     main timestamp (16 bit)
     message type (8 bit) 1=TA,2=UPDT
     message size (8 bit)
     [ts_msg] (16 bit)
   */
  uint16_t *main_ts;
  uint8_t *size, *type, i;
  ts_msg *ts;
//save current timestamps array in history
  for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
    {
      net_ts_history[this_id][i] = net_ts[i];
    }
//re-set timer (resend TA at fixed intervals)
//  ctimer_set (&broadcasttimer, CLOCK_SECOND * 5, send_bcast, NULL);
//update statistics
  soft_size_beacon +=
    sizeof (ts_msg) * (reach_num ()) + sizeof (uint16_t) +
    sizeof (uint8_t) * 2 + soft_broadcast_header_size;
  soft_num_beacon++;
//start to print timestamps array
  PRINTF ("TA(%d)=[", rimeaddr_node_addr.u8[0]);
//clear packet buffer and set packet size
  packetbuf_clear ();
  packetbuf_set_datalen (sizeof (ts_msg) * (reach_num ()) +
			 sizeof (uint16_t) + sizeof (uint8_t) * 2);
//get pointer to packet and header fields
  main_ts = packetbuf_dataptr ();
  type = (uint8_t *) (main_ts + 1);
  size = (uint8_t *) (type + 1);
  ts = (ts_msg *) (size + 1);
//populate packet header fields (type,size,timestamp of packet sender)
  *main_ts = max_ts;
  *size = reach_num ();
  *type = 1;
//populate packet dynamic part (known timestamp of other nodes)
  for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
    {
	  //PRINTF ("%d:%d,", i, net_ts[i]);
      if (net_ts[i] + max_time > max_ts)
	{
	  //print timestamp in the array
	  PRINTF ("%d:%d,", i, net_ts[i]);
	  ts->timestamp = (*main_ts) - get_ts (i);
	  ts->addr = i;
	  ts++;
	}
    }
  PRINTF ("]\n");
//finally send the packet
  broadcast_send (&bcast);
}

static void
send_UA ()
{
  /*
     main timestamp (16 bit)
     message type (8 bit) 1=TA,2=UPDT
     message size (8 bit)
     [ts_msg] (16 bit)
   */
  uint16_t *main_ts;
  uint8_t *size, *type, i;
  ts_msg *ts;
//DO NOT save current timestamps array in history
//re-set timer (resend TA at fixed intervals)
//  ctimer_set (&broadcasttimer, CLOCK_SECOND * 5, send_bcast, NULL);
//update statistics
  soft_size_beacon +=
    sizeof (ts_msg) * (inc_reach_num ()) + sizeof (uint16_t) +
    sizeof (uint8_t) * 2 + soft_broadcast_header_size;
  soft_num_beacon++;
//start to print timestamps array
  PRINTF ("UP(%d)=[", rimeaddr_node_addr.u8[0]);
//clear packet buffer and set packet size
  packetbuf_clear ();
  packetbuf_set_datalen (sizeof (ts_msg) * (inc_reach_num ()) +
			 sizeof (uint16_t) + sizeof (uint8_t) * 2);
//get pointer to packet and header fields
  main_ts = packetbuf_dataptr ();
  type = (uint8_t *) (main_ts + 1);
  size = (uint8_t *) (type + 1);
  ts = (ts_msg *) (size + 1);
//populate packet header fields (type,size,timestamp of packet sender)
  *main_ts = max_ts;
  *size = inc_reach_num ();
  *type = 2;
//populate packet dynamic part (known timestamp of other nodes)
  for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
    {
      //add all nodes included in the last complete timestamp array AND offset ids different from 0 
      if (net_ts_history[this_id][i] + max_time >
	  net_ts_history[this_id][this_id])
	{
	  //this is an update message, send timestamp only if offset from the node timestamp is different from 0. to have a view of the connected node you have to wait a full message. this messages instead are only updates of the timestamp.
	  if (net_ts[i] != max_ts)
	    {
	      //print timestamp in the array
	      PRINTF ("%d:%d,", i, net_ts[i]);
	      ts->timestamp = (*main_ts) - get_ts (i);
	      ts->addr = i;
	      ts++;
	    }
	}
      else
	{
	  //add also timestamps of nodes that where not in the last timestamp array and are alive (new nodes from the last TA)
	  if (net_ts[i] + max_time > max_ts)
	    {
	      //print timestamp in the array
	      PRINTF ("%d:%d,", i, net_ts[i]);
	      ts->timestamp = (*main_ts) - get_ts (i);
	      ts->addr = i;
	      ts++;
	    }
	}
    }
  PRINTF ("]\n");
//finally send the packet
  broadcast_send (&bcast);
}

void
periodic_bcast()
{
turn ++;
  ctimer_set (&broadcasttimer, CLOCK_SECOND * 5, periodic_bcast, NULL);

if (turn%30 == 0 || turn < 20) send_TA();
else send_UA();
//send_TA();
}

void
adv_send (struct neighbor_discovery_conn *c)
{
  //send_bcast();
  //PRINTF ("sent adv\n\n");
}


static const struct neighbor_discovery_callbacks neighbor_discovery_callbacks
  = { adv_received, adv_send };
static const struct neighbor_callbacks neighbor_callbacks = { node_rm };
static const struct broadcast_callbacks bcast_callbacks = { recv_bcast };

/*---------------------------------------------------------------------------*/
PROCESS_THREAD (example_polite_process, ev, data)
{
  static struct neighbor_discovery_conn c;
  int i, j;

  PROCESS_EXITHANDLER (neighbor_discovery_close (&c));
  PROCESS_BEGIN ();
  /* Neighbor discovery init */
  neighbor_init_calls (&neighbor_callbacks);
  broadcast_open (&bcast, BCAST_CHANNEL, &bcast_callbacks);
  neighbor_discovery_open (&c, NEIGHBOR_DISCOVERY_CHANNEL, CLOCK_SECOND * 10,
			   CLOCK_SECOND * BEACON_RATE, CLOCK_SECOND * (BEACON_RATE+1),
			   &neighbor_discovery_callbacks);
  neighbor_discovery_start (&c, 100);

  for (i = 0; i < MAX_REACHABLE_NEIGHBORS; i++)
    {
      net_ts[i] = 0;
      for (j = 0; j < MAX_REACHABLE_NEIGHBORS; j++)
	{
	  net_ts_history[j][i] = 0;
	}
    }

  this_id = rimeaddr_node_addr.u8[0] - 1;

  ctimer_set (&t, CLOCK_SECOND * 20, periodic, NULL);
  ctimer_set (&broadcasttimer,
	      CLOCK_SECOND + random_rand () % (5 * CLOCK_SECOND), periodic_bcast,
	      NULL);

  while (1)
    {
      static struct etimer et;
      etimer_set (&et, CLOCK_SECOND * 10);
      PROCESS_WAIT_EVENT_UNTIL (etimer_expired (&et));
/*PRINTF ("%d.%d:sent num:msgs:%ld:ack:%ld:beacons:%ld:total:%ld\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1], soft_num_msg,soft_num_ack,soft_num_beacon,soft_num_msg+soft_num_ack+soft_num_beacon);*/
    printf ("%d.%d:SS:%ld:%ld:%ld:%ld:%ld\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1], soft_size_msg,soft_size_ack,soft_size_beacon,soft_size_msg+soft_size_ack+soft_size_beacon,recv_size);
    //update net size
    printf ("%d.%d:NS:%d\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1], reach_num());

      /*PRINTF ("%d.%d:alive:%d:%d\n", rimeaddr_node_addr.u8[0],
         rimeaddr_node_addr.u8[1], rimeaddr_node_addr.u8[0],
         rimeaddr_node_addr.u8[1]); */
    }
  PROCESS_END ();
}

/*---------------------------------------------------------------------------*/
