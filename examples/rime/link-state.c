#include "contiki.h"
#include "lib/list.h"
#include "lib/memb.h"
#include "net/rime/neighbor.h"
#include "link-state.h"
#include "net/rime/neighbor-discovery.h"
#include "net/rime/broadcast.h"
#include "lib/random.h"
#include "dev/watchdog.h"
#include "net/mac/mac.h"
#include "net/mac/nullmac.h"
#include "script-header.h"

#define DEBUG 1
#if DEBUG
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif

#define DEC2FIX(h,d) ((h * 64L) + (unsigned long)((d * 64L) / 1000L))

//memory space for lists declarations
MEMB (reachable_neighbor_mem, struct reachable_neighbor,
      MAX_REACHABLE_NEIGHBORS);
MEMB (msg_queue_mem, struct msg_queue, MAX_QUEUE_SIZE);
MEMB (tobesent_ack_mem, struct tobesent_ack, MAX_NEIGHBORS);
MEMB (waiting_ack_mem, struct waiting_ack, MAX_NEIGHBORS);
MEMB (reach_set_mem, struct reach_set, MAX_REACHABLE_NEIGHBORS);
//lists declarations
LIST (reachable_neighbor_list);
LIST (msg_queue_list);
LIST (tobesent_ack_list);
LIST (waiting_ack_list);
LIST (reach_set_list);

//broadcast status variables
static struct broadcast_conn bcast;
static struct broadcast_conn bcastACK;
//static int net_size = 0;
//variables for statistics
long num_msg = 0;
long size_msg = 0;
long num_ack = 0;
long size_ack = 0;
long num_rem = 0;
long num_add =0;
long num_beacon = 0;
long size_beacon = 0;
long recv_size = 0;
long broadcast_header_size =
  sizeof (struct packetbuf_attr) + sizeof (struct packetbuf_addr);
//static int waiting_ack = 0;
//retransmission mechanism variables
static uint8_t last_msg_size = 0;
static int retries = 0;
//ack backoff timer
struct ctimer ack_bo;
//information output timer
struct ctimer print_info_timer;
//resent links timer
struct ctimer update_timer;
static int new_node = 0;
//energy statistics
//static struct energy_time last;
//static struct energy_time diff;
// FIXME: workaround to turn on/off radio. Rime should export an MAC on/off interface to avoid forcing the user to do this explicitly
//static struct mac_driver *mac = &nullmac_driver;

/*
static uint8_t ack_size;
static rimeaddr_t ack_sender;
static int sending_ack = 0;
*/

/*---------------------------------------------------------------------------*/
PROCESS (example_polite_process, "");
AUTOSTART_PROCESSES (&example_polite_process);
/*---------------------------------------------------------------------------*/




struct reachable_neighbor *
node_find (const rimeaddr_t * node_addr)
{
  struct reachable_neighbor *n;
  for (n = list_head (reachable_neighbor_list); n != NULL; n = n->next)
    {
      if (rimeaddr_cmp (&n->node_addr, node_addr))
	{
	  return n;
	}
    }
  return NULL;
}

int
reach_set_find (const rimeaddr_t * node_addr)
{
  struct reach_set *rs;

  for (rs = list_head (reach_set_list); rs != NULL; rs = rs->next)
    {
      if (rimeaddr_cmp (&rs->node_addr, node_addr))
	{
	  return 1;
	}
    }
  return 0;
}

int
build_reachable_set ()
{
  struct paths_list *p;
  struct reachable_neighbor *rn;
  struct reach_set *rs, *rs_temp;

  watchdog_stop ();
  memb_init (&reach_set_mem);
  list_init (reach_set_list);
  //add myself to reach set
  rs = memb_alloc (&reach_set_mem);
  if (rs != NULL)
    {
      list_add (reach_set_list, rs);
      rimeaddr_copy (&rs->node_addr, &rimeaddr_node_addr);
    }
  //for every node in the reach_set add its neighbors (if not already in the reach_set)
  /*PRINTF ("%d.%d:reach_set", rimeaddr_node_addr.u8[0],
	  rimeaddr_node_addr.u8[1]);*/
  for (rs_temp = list_head (reach_set_list); rs_temp != NULL;
       rs_temp = rs_temp->next)
    {
      rn = node_find (&rs_temp->node_addr);
      if (rn != NULL)
	{
	  //add all node neighbors to the tail of reach_set
	  for (p = list_head (rn->node_paths); p != NULL; p = p->next)
	    {
	      if (reach_set_find (&p->node_addr) == 0)
		{
		  rs = memb_alloc (&reach_set_mem);
		  if (rs != NULL)
		    {
		      list_add (reach_set_list, rs);
		      rimeaddr_copy (&rs->node_addr, &p->node_addr);
		      /*PRINTF (":%d.%d", p->node_addr.u8[0],
			      p->node_addr.u8[1]);*/
		    }
		}
	    }
	}
    }
  //PRINTF ("\n");
  watchdog_start ();
  return list_length (reach_set_list);
}



static void
queue_message (const rimeaddr_t * node_reach, const rimeaddr_t * path,
	       char operation)
{
  struct msg_queue *m, *temp_m, *temp_mm;
  //remove all previous messages to the same dest and with the same node_reach
  temp_m = NULL;
  for (m = list_head (msg_queue_list); m != NULL;)
    {
      if ((rimeaddr_cmp (node_reach, &(&m->msg)->node_addr)
	  && rimeaddr_cmp (path, &(&m->msg)->path_addr)) || (rimeaddr_cmp (path, &(&m->msg)->node_addr)
	  && rimeaddr_cmp (node_reach, &(&m->msg)->path_addr)))
	{
	  if (temp_m == NULL)
	    {
	      *msg_queue_list = m->next;
	    }
	  else
	    {
	      temp_m->next = m->next;
	    }
	  //delete item from memory
	  temp_mm = m;
	  //m = m->next;
	  memb_free (&msg_queue_mem, temp_mm);
	  break;
	}
      else
	{
	  temp_m = m;
	  m = m->next;
	}
    }
  m = NULL;
  m = memb_alloc (&msg_queue_mem);
  if (m != NULL)
    {
      list_add (msg_queue_list, m);
      (&m->msg)->operation = operation;
      rimeaddr_copy (&(&m->msg)->node_addr, node_reach);
      rimeaddr_copy (&(&m->msg)->path_addr, path);
      /*PRINTF ("%d.%d:queue:%c:%d.%d:path:%d.%d\n", rimeaddr_node_addr.u8[0],
         rimeaddr_node_addr.u8[1], operation, node_reach->u8[0],
         node_reach->u8[1], path->u8[0], path->u8[1]); */
    }
  else
    {
      PRINTF ("%d.%d:queue:full\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1]);
    }
}

static void
send_reachable (void)
{
  /*
     message structure
     Message type = 1 (8 bit)
     Number of updates (8 bit)
     -----
     reach_msg
     -----
   */
  struct msg_queue *m;
  struct reach_msg *msg;
  struct neighbor *n;
  struct waiting_ack *ack;
  uint8_t *size;
  uint8_t msgs_count = 0;
  //we want to sen more than one type of message. we use 8 bit to distinguish the type. 1 = incremental updates
  uint8_t msg_type = 1;
  int i;

//stop after MAX_RETRANSMISSIONS retries
  if (retries > MAX_RETRANSMISSIONS)
    {
      for (i = 0; i < last_msg_size; i++)
	{
	  memb_free (&msg_queue_mem, list_pop (msg_queue_list));
	}
      last_msg_size = 0;
      retries = 0;
      PRINTF ("%d.%d:max reties reached\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1]);
    }

  //send MAX_MSG_SIZE operations per messages. First count the number of these messages.
  msgs_count = list_length (msg_queue_list);
  //if message list is empty, return
  if (msgs_count == 0)
    return;
  if (msgs_count > MAX_MSG_SIZE)
    msgs_count = MAX_MSG_SIZE;
  if (last_msg_size > 0 && msgs_count >= last_msg_size)
    msgs_count = last_msg_size;
  //max MAX_MSG_SIZE ops per message
  /*PRINTF ("%d.%d:sending_multiple_msgs_size:%d\n", rimeaddr_node_addr.u8[0],
     rimeaddr_node_addr.u8[1], msgs_count); */
  //then send
  packetbuf_clear ();
  size_msg +=
    sizeof (struct reach_msg) * (msgs_count) + sizeof (uint16_t) +
    broadcast_header_size;
  num_msg++;
  packetbuf_set_datalen (sizeof (struct reach_msg) * (msgs_count) +
			 sizeof (uint8_t) * 2);
  size = packetbuf_dataptr ();
  msg = (struct reach_msg *) (size + 2);
  //message type
  *size = msg_type;
  //message size
  *(size + 1) = msgs_count;
  //read from queue but not pop out (will be popped out when ack received)
  m = list_head (msg_queue_list);
  for (i = 0; i < msgs_count; i++)
    {
      //m = list_pop (msg_queue_list);
      //add item to the message
      msg->operation = (&m->msg)->operation;
      rimeaddr_copy (&msg->node_addr, &(&m->msg)->node_addr);
      rimeaddr_copy (&msg->path_addr, &(&m->msg)->path_addr);
      msg++;
      //read from queue but not pop out (will be popped out when ack received)
      m = m->next;
    }
  broadcast_send (&bcast);
  //wait for acks
  last_msg_size = msgs_count;
  if (list_length (waiting_ack_list) == 0)
    {
      for (n = list_head (get_neighbors ()); n != NULL; n = n->next)
	{
	  ack = memb_alloc (&waiting_ack_mem);
	  list_add (waiting_ack_list, ack);
	  rimeaddr_copy (&ack->receiver, &n->addr);
	  //waiting_ack = 1;
	  //waiting_ack = (list_length (get_neighbors ()));
	}
    }
  else
    {
      retries++;
    }

  /*PRINTF ("%d.%d:msg_count:%ld:total sent size.sent messages:%ld\n",
	  rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], size_msg,
	  num_msg);*/
}

static void
rem_reachable (const rimeaddr_t * neighbor, const rimeaddr_t * node)
{

  struct reachable_neighbor *n;
  struct paths_list *p;

  // Check if the path is in list. 
  n = node_find (node);
  if (n == NULL)
    {
      // If the node was not on the list we can not remove, so exit 
      return;
    }
  // If the neighbor is list
  for (p = list_head (n->node_paths); p != NULL; p = p->next)
    {
      if (rimeaddr_cmp (&(p->node_addr), neighbor))
	{
	  break;
	}
    }
  // If the neighbor was not on the list, we can not remove, so exit 
  if (p == NULL)
    {
      //neighbour already NOT on list. With a certain probability (0.5) retransmit
      //if (random_rand() % 4  > 1) 
      //queue_message (neighbor, node, 'r');
      return;
    }
  // we remove the neighbor 
  list_remove (n->node_paths, p);
  memb_free (&n->paths_list_mem, p);
  //removed neighbor, tell to neighbor.
  queue_message (neighbor, node, 'r');
  num_rem ++;

}


static void
add_reachable (const rimeaddr_t * neighbor, const rimeaddr_t * node)
{
  struct reachable_neighbor *n;
  struct paths_list *p;

  n = node_find (node);
  if (n == NULL)
    {
      /*PRINTF ("%d.%d: reachable_add: not on list, allocating\n",
         rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1]); */
      n = memb_alloc (&reachable_neighbor_mem);
      if (n != NULL)
	{
	  list_add (reachable_neighbor_list, n);
	  //node init
	  n->node_paths_list = NULL;
	  rimeaddr_copy (&n->node_addr, node);
	  n->node_paths = (list_t) & n->node_paths_list;
	  n->paths_list_mem.size = sizeof (struct paths_list);
	  n->paths_list_mem.num = MAX_NEIGHBORS;
	  n->paths_list_mem.count = n->paths_list_mem_memb_count;
	  n->paths_list_mem.mem = (void *) n->paths_list_mem_memb_mem;
	  memb_init (&n->paths_list_mem);
	  list_init (n->node_paths);
	  /*PRINTF ("%d.%d:add_reach:%d.%d:add to reach\n",
	     rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1],
	     node_addr->u8[0], node_addr->u8[1]); */
	}
    }
  if (n != NULL)
    {
      //PRINTF("%d.%d: reachable_add: start adding path\n",rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1]);
      //add the neighbor to the path node
      watchdog_stop ();
      for (p = list_head (n->node_paths); p != NULL; p = p->next)
	{
	  if (rimeaddr_cmp (&p->node_addr, neighbor))
	    {
	      break;
	    }
	}
      watchdog_start ();
      /* If the neighbor was not on the list, we try to allocate memory
         for it. */
      if (p == NULL)
	{
	  p = memb_alloc (&n->paths_list_mem);
	  if (p != NULL)
	    {
	      rimeaddr_copy (&p->node_addr, neighbor);
	      list_add (n->node_paths, p);
	      //new path, tell to neighbor.
	      queue_message (neighbor, node, 'a');
	      num_add ++;
	    }
	}
      else
	{
	  //neighbour already on list. With a certain probability (0.5) retransmit
	  //if (random_rand() % 4  > 1) 
	  //queue_message (node_addr, path, 'a');
	}
    }

}

void
adv_received (struct neighbor_discovery_conn *c, const rimeaddr_t * from,
	      uint16_t val)
{
  struct neighbor *n;
  //is possible that I receive a message frome someone not in my node_list
  //add_reachable (from, &rimeaddr_node_addr);
  recv_size += sizeof (struct adv_msg) + broadcast_header_size;
  n = neighbor_find (from);
  if (n == NULL)
    {
      //if the new neighbor is also a new reachable node I set the new node flag (I will send all the network topology)
      if (reach_set_find(from)==0) new_node = 1;
      //new_node = 1;
      neighbor_add (from, 100, 1);
      n = neighbor_find (from);
      add_reachable (from, &rimeaddr_node_addr);
      add_reachable (&rimeaddr_node_addr,from);
      //new neighbor node, tell to this new node all my reachable nodes
      /*for (rn = list_head (reachable_neighbor_list); rn != NULL;
         rn = rn->next)
         {
         for (p = list_head (rn->node_paths); p != NULL; p = p->next)
         {
         //queue_message (&rn->node_addr, &p->node_addr, 'a');
         PRINTF
         ("%d.%d:updt_my_new_neighbor:%d.%d:add:%d.%d:path:%d.%d\n",
         rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1],
         n->addr.u8[0], n->addr.u8[1], rn->node_addr.u8[0],
         rn->node_addr.u8[1], p->node_addr.u8[0], p->node_addr.u8[1]);
         }
         } */
      /*PRINTF ("%d.%d:add_neighbor:%d.%d:total:%d\n", rimeaddr_node_addr.u8[0],
         rimeaddr_node_addr.u8[1], from->u8[0], from->u8[1],
         neighbor_num ()); */
    }
  else
    {
      neighbor_update (n, 100);
      /*PRINTF ("%d.%d:updt_neighbor:%d.%d:total:%d\n",
         rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], from->u8[0],
         from->u8[1], neighbor_num ()); */
    }
}

void
adv_send (struct neighbor_discovery_conn *c)
{
  //PRINTF ("%d.%d:send_adv\n", rimeaddr_node_addr.u8[0],rimeaddr_node_addr.u8[1]);
  num_beacon++;
  size_beacon += sizeof (struct adv_msg) + broadcast_header_size;
}



static void
node_rm (rimeaddr_t addr, struct neighbor *nn)
{
  struct waiting_ack *n;
  int i;

  /*PRINTF ("%d.%d:rem_neighbor:%d.%d:total:%d\n", rimeaddr_node_addr.u8[0],
     rimeaddr_node_addr.u8[1], addr.u8[0], addr.u8[1], neighbor_num ()); */
  watchdog_stop ();
  //remove the node from waiting_ack_list 
  if (list_length (waiting_ack_list) > 0)
    {
      for (n = list_head (waiting_ack_list); n != NULL; n = n->next)
	{
	  if (rimeaddr_cmp (&n->receiver, &addr))
	    {
	      break;
	    }
	}
      if (n != NULL)
	{
	  list_remove (waiting_ack_list, n);
	  memb_free (&waiting_ack_mem, n);
	}
      if (list_length (waiting_ack_list) == 0)
	{
	  for (i = 0; i < last_msg_size; i++)
	    {
	      memb_free (&msg_queue_mem, list_pop (msg_queue_list));
	    }
	  last_msg_size = 0;
	  retries = 0;
	}
    }
//remove edge (node-me) from the graph
  rem_reachable (&addr, &rimeaddr_node_addr);
  rem_reachable ( &rimeaddr_node_addr,&addr);
  watchdog_start ();
}


void
send_ack (void *ptr)
{
  struct tobesent_ack *ack;
  rimeaddr_t *ack_sender_addr;
  uint8_t *size, msg_type;

  ack = (struct tobesent_ack *) ptr;

//send a ack to the broadcast
  packetbuf_clear ();
  packetbuf_set_datalen (sizeof (rimeaddr_t) + sizeof (uint8_t) * 2);
  size_ack +=
    sizeof (rimeaddr_t) + sizeof (uint8_t) * 2 + broadcast_header_size;
  num_ack++;
  size = packetbuf_dataptr ();
  ack_sender_addr = (rimeaddr_t *) (size + 1);
  //message type
  msg_type = 2;
  *size = msg_type;
  //broadcast sender
  rimeaddr_copy (ack_sender_addr, &ack->sender);
  //send
  broadcast_send (&bcastACK);
  /*printf ("%d.%d:sending ack to:%d.%d\n",
     rimeaddr_node_addr.u8[0],
     rimeaddr_node_addr.u8[1], ack->sender.u8[0], ack->sender.u8[1]); */
  list_remove (tobesent_ack_list, ack);
  memb_free (&tobesent_ack_mem, ack);

}




static void
recv_bcast (struct broadcast_conn *broadcast, const rimeaddr_t * from)
{

  struct reach_msg *msg;
  struct tobesent_ack *ack;
  uint8_t *size, msg_type, sz, i;
  /*
     INCREMENTAL UPDATE message structure
     Message type = 1 (8 bit)
     Number of updates (8 bit)
     -----
     reach_msg
     -----
   */

  //the message is considered also as a beacon
  adv_received (NULL, from, 0);

  size = packetbuf_dataptr ();
  msg_type = *size;
  sz = *(size + 1);
  /*printf ("%d.%d:msg_rcv:from:%d.%d:type:%d:size:%d:data",
     rimeaddr_node_addr.u8[0],
     rimeaddr_node_addr.u8[1], from->u8[0], from->u8[1], msg_type, sz); */

  //if the message is a ACK, remove the messages acked and then exit (now we do not wait for acks)
  if (msg_type == 2)
    {
      return;
    }

  msg = (struct reach_msg *) (size + 2);
  for (i = 0; i < sz; i++)
    {
      /*printf (":%c:%d.%d:path:%d.%d", msg[i].operation,
         msg[i].node_addr.u8[0], msg[i].node_addr.u8[1],
         msg[i].path_addr.u8[0], msg[i].path_addr.u8[1]); */
    }
  /*printf ("\n"); */
  recv_size +=
    sizeof (struct reach_msg) * (sz) + sizeof (uint16_t) +
    broadcast_header_size;

  for (i = 0; i < sz; i++)
    {
      //consider messages not about myself
      //if(rimeaddr_cmp(&msg[i].node_addr,&rimeaddr_node_addr)) break;
      //if(rimeaddr_cmp(&msg[i].path_addr,&rimeaddr_node_addr)) break;
      if (msg[i].operation == 'a')
	{
	  if (msg[i].node_addr.u8[1] + msg[i].path_addr.u8[1] > 0)
	    continue;
	  add_reachable (&msg[i].node_addr, &msg[i].path_addr);
	  add_reachable (&msg[i].path_addr,&msg[i].node_addr);
	}
      if (msg[i].operation == 'r')
	{
	  rem_reachable (&msg[i].node_addr, &msg[i].path_addr);
	  rem_reachable (&msg[i].path_addr,&msg[i].node_addr);
	}
    }

  //send ack in a random time 
  ack = memb_alloc (&tobesent_ack_mem);
  if (ack != NULL)
    {
      list_add (tobesent_ack_list, ack);
      rimeaddr_copy (&ack->sender, from);
      ctimer_set (&ack_bo, random_rand () % (CLOCK_SECOND * 1),
		  send_ack, ack);
    }

}

static void
recv_ack (struct broadcast_conn *broadcast, const rimeaddr_t * from)
{
  struct waiting_ack *n;
  uint8_t *size, msg_type, i;
  rimeaddr_t *ack_sender_addr;

  /*
     ACK message structure
     Message type = 2 (8 bit)
     Address of the sender
   */

recv_size +=
    sizeof (rimeaddr_t) + sizeof (uint8_t) * 2 + broadcast_header_size;

  //the message is considered also as a beacon
  adv_received (NULL, from, 0);

  size = packetbuf_dataptr ();
  msg_type = *size;
  /*PRINTF ("%d.%d:ack_rcv:from:%d.%d",
     rimeaddr_node_addr.u8[0],
     rimeaddr_node_addr.u8[1], from->u8[0], from->u8[1]); */

  //if the message is a ACK, remove the messages acked and then exit (now we do not wait for acks)
  if (msg_type == 2)
    {
      //if (waiting_ack == 1)
      if (list_length (waiting_ack_list) > 0)
	{
	  ack_sender_addr = (rimeaddr_t *) (size + 1);
	  //check if the ack is for my broadcast, if yes, remove from waiting ack
	  if (rimeaddr_cmp (ack_sender_addr, &rimeaddr_node_addr))
	    {
	      //find ack sender in ack waiting list
	      for (n = list_head (waiting_ack_list); n != NULL; n = n->next)
		{
		  /*printf (":scan:%d", n->receiver.u8[0]); */
		  if (rimeaddr_cmp (&n->receiver, from))
		    {
		      break;
		    }
		}
	      if (n != NULL)
		{
		  list_remove (waiting_ack_list, n);
		  memb_free (&waiting_ack_mem, n);
		}
	      /*printf (":Still missing:%d", list_length (waiting_ack_list)); */
	      //delete messages acked
	      if (list_length (waiting_ack_list) == 0)
		{
		  for (i = 0; i < last_msg_size; i++)
		    {
		      memb_free (&msg_queue_mem, list_pop (msg_queue_list));
		    }
		  last_msg_size = 0;
		  retries = 0;
		}
	      //waiting_ack = 0;
	      //waiting_ack--;
	    }
	}
    }

  /*printf ("\n"); */
}

void
print_info ()
{

  //unsigned long avg_power;
  //unsigned long time;

  //update net size
  PRINTF ("%d.%d:NS:%d\n", rimeaddr_node_addr.u8[0],
	  rimeaddr_node_addr.u8[1], build_reachable_set ());
  /*PRINTF ("%d.%d:net_size:%d:neighbors:%d:add_edges:%ld:rem_edges:%ld\n", rimeaddr_node_addr.u8[0],
	  rimeaddr_node_addr.u8[1], build_reachable_set (),
	  list_length (get_neighbors ()),num_add,num_rem);

  PRINTF ("%d.%d:alive:%d:%d\n", rimeaddr_node_addr.u8[0],
	  rimeaddr_node_addr.u8[1], rimeaddr_node_addr.u8[0],
	  rimeaddr_node_addr.u8[1]);
  PRINTF ("%d.%d:sent num:msgs:%ld:ack:%ld:beacons:%ld:total:%ld\n",
	  rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], num_msg,
	  num_ack, num_beacon, num_msg + num_ack + num_beacon);
  PRINTF ("%d.%d:sent sizes:msgs:%ld:ack:%ld:beacons:%ld:total:%ld:received:%ld\n",
	  rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], size_msg,
	  size_ack, size_beacon, size_msg + size_ack + size_beacon,recv_size);*/
PRINTF ("%d.%d:SS:%ld:%ld:%ld:%ld:%ld\n",
	  rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], size_msg,
	  size_ack, size_beacon, size_msg + size_ack + size_beacon,recv_size);
  /* stop-start ongoing time measurements to retrieve the diffs
     during last interval */
  /*ENERGEST_OFF (ENERGEST_TYPE_CPU);
  ENERGEST_ON (ENERGEST_TYPE_CPU);
  

  // Energy time diff 
  diff.cpu = energest_type_time (ENERGEST_TYPE_CPU) - last.cpu;
  diff.lpm = energest_type_time (ENERGEST_TYPE_LPM) - last.lpm;
  diff.transmit = energest_type_time (ENERGEST_TYPE_TRANSMIT) - last.transmit;
  diff.listen = energest_type_time (ENERGEST_TYPE_LISTEN) - last.listen;
  

  time = diff.cpu + diff.lpm;
  
  
  PRINTF("%d.%d:energy times:CPU:%d:%%:LPM:%d:%%:tx:%d:%%:rx:%d:%%:tot:%lu:mJ\n",
	   rimeaddr_node_addr.u8[0], 
	   rimeaddr_node_addr.u8[1],
	   (int)((100L * (unsigned long)diff.cpu) / time),
	   (int)((100L * (unsigned long)diff.lpm) / time),
	   (int)((100L * (unsigned long)diff.transmit) / time),
	   (int)((100L * (unsigned long)diff.listen) / time),
	   (unsigned long) ((1.8 * diff.cpu + 20.0 * diff.listen + 
		17.7 * diff.transmit) * 3 / RTIMER_SECOND));
*/
  /*PRINTF ("%d.%d:energy times:differential:cpu:%ld:lpm:%ld:transmit:%ld:listen:%ld\n", rimeaddr_node_addr.u8[0],
     rimeaddr_node_addr.u8[1], diff.cpu,diff.lpm,diff.transmit,diff.listen); */
  ctimer_set (&print_info_timer, CLOCK_SECOND * 10, print_info, NULL);
}

void
update ()
{
  //struct reach_set *rs;
  struct reachable_neighbor *rn;
  struct paths_list *p;
//restart the update timer
  ctimer_set (&update_timer, CLOCK_SECOND * 30, update, NULL);
//clear from the topology nodes that are not more reachable
  for (rn = list_head (reachable_neighbor_list); rn != NULL; rn = rn->next)
    {
      if (reach_set_find (&rn->node_addr) == 0)
	{
	  /*PRINTF
	     ("%d.%d:removing from graph:%d.%d\n",
	     rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1],
	     rn->node_addr.u8[0], rn->node_addr.u8[1]);*/
	  list_remove (reachable_neighbor_list, rn);
	  memb_free (&reachable_neighbor_mem, rn);
	}
    }
//if new_node == 0 then there are no new nodes in the last 30 second
  if (new_node == 0)
    return;
//new neighbor nodes, tell net topology (only of reachable nodes)
  for (rn = list_head (reachable_neighbor_list); rn != NULL; rn = rn->next)
    {
      for (p = list_head (rn->node_paths); p != NULL; p = p->next)
	{
	  queue_message (&p->node_addr, &rn->node_addr, 'a');
	  /*PRINTF
	     ("%d.%d:updt_my_new_neighbor:%d.%d:add:%d.%d:path:%d.%d\n",
	     rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1],
	     n->addr.u8[0], n->addr.u8[1], rn->node_addr.u8[0],
	     rn->node_addr.u8[1], p->node_addr.u8[0], p->node_addr.u8[1]); */
	}
    }
//reset the new_node flag
  new_node = 0;

}

static const struct neighbor_discovery_callbacks neighbor_discovery_callbacks
  = { adv_received, adv_send };
static const struct neighbor_callbacks neighbor_callbacks = { node_rm };
static const struct broadcast_callbacks bcast_callbacks = { recv_bcast };
static const struct broadcast_callbacks ack_callbacks = { recv_ack };

/*---------------------------------------------------------------------------*/
PROCESS_THREAD (example_polite_process, ev, data)
{
  static struct neighbor_discovery_conn c;

  PROCESS_EXITHANDLER (neighbor_discovery_close (&c));
  PROCESS_BEGIN ();
  /* Lists init */
  memb_init (&reachable_neighbor_mem);
  memb_init (&msg_queue_mem);
  memb_init (&tobesent_ack_mem);
  memb_init (&waiting_ack_mem);
  memb_init (&reach_set_mem);
  list_init (reachable_neighbor_list);
  list_init (msg_queue_list);
  list_init (tobesent_ack_list);
  list_init (waiting_ack_list);
  list_init (reach_set_list);
  /* Energy time init */
  /*
  energest_init ();
  last.cpu = energest_type_time (ENERGEST_TYPE_CPU);
  last.lpm = energest_type_time (ENERGEST_TYPE_LPM);
  last.transmit = energest_type_time (ENERGEST_TYPE_TRANSMIT);
  last.listen = energest_type_time (ENERGEST_TYPE_LISTEN);
  */
  /* Broadcast and neighbor discovery init */
  neighbor_init_calls (&neighbor_callbacks);
  broadcast_open (&bcast, BCAST_CHANNEL, &bcast_callbacks);
  broadcast_open (&bcastACK, ACK_CHANNEL, &ack_callbacks);
  neighbor_discovery_open (&c, NEIGHBOR_DISCOVERY_CHANNEL, CLOCK_SECOND * 10,
			   CLOCK_SECOND * BEACON_RATE, CLOCK_SECOND * (BEACON_RATE+1),
			   &neighbor_discovery_callbacks);
  neighbor_discovery_start (&c, 100);
  /* Random init */
  random_init (rimeaddr_node_addr.u8[0]);
  /* Information output init */
  ctimer_set (&print_info_timer, CLOCK_SECOND * 10, print_info, NULL);
  /* Update init */
  ctimer_set (&update_timer, CLOCK_SECOND * 30, update, NULL);
  while (1)
    {
      static struct etimer et;
      etimer_set (&et,
		  3 * CLOCK_SECOND / 2 + random_rand () % (2 * CLOCK_SECOND));
      //etimer_set (&et, CLOCK_SECOND * 5 );
      PROCESS_WAIT_EVENT_UNTIL (etimer_expired (&et));
      send_reachable ();
    }
  PROCESS_END ();
}

/*---------------------------------------------------------------------------*/
