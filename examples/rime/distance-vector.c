#include "contiki.h"
#include "net/rime/neighbor.h"
#include "net/rime/neighbor-discovery.h"
#include "distance-vector.h"
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

//distance_vector[path][reachablenode] = hop distance to reachable node
uint8_t dv[MAX_REACHABLE_NEIGHBORS+1][MAX_REACHABLE_NEIGHBORS+1];
uint8_t last_dv[MAX_REACHABLE_NEIGHBORS +1];
uint8_t this_id = 0;
uint16_t seq_num = 1;
uint8_t outdated = 0;
uint16_t recv_seq_num[MAX_REACHABLE_NEIGHBORS +1];

static struct ctimer t;
static struct ctimer broadcasttimer;
static struct broadcast_conn bcast;
static struct broadcast_conn bcast2;
static long num_msg = 0;
static long size_msg = 0;
static long num_ack = 0;
static long size_ack = 0;
static long num_beacon = 0;
static long size_beacon = 0;
static long recv_size = 0;
static long soft_broadcast_header_size =
  sizeof (struct packetbuf_attr) + sizeof (struct packetbuf_addr);

/*---------------------------------------------------------------------------*/
PROCESS (example_polite_process, "");
AUTOSTART_PROCESSES (&example_polite_process);
/*---------------------------------------------------------------------------*/
int
check_changes(){
int i,j;

for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++)
    {
      uint8_t min_distance = MAX_DISTANCE;
      //search for shortest distance
      for (j = 1; j <= MAX_REACHABLE_NEIGHBORS; j++){
        if (dv[j][i] < min_distance) {
          min_distance = dv[j][i];
        }
      }
      if (min_distance != last_dv[i]){
        PRINTF("found changes in the DV\n");
        return 1;
        }
      }
    return 0;
}

int
reach_num ()
{
  int tot = 0;
  int i,j;
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++)
    {
      uint8_t min_distance = MAX_DISTANCE;
      for (j = 1; j <= MAX_REACHABLE_NEIGHBORS; j++){
        if (dv[j][i] < min_distance) {
          min_distance = dv[j][i];
        }
      }
      if(min_distance < MAX_DISTANCE){
       tot++;
      }
    }
  return tot;
}





static void
adv_received (struct neighbor_discovery_conn *c, const rimeaddr_t * from,
	      uint16_t val)
{
  uint8_t sender_id;
  struct neighbor *n;
  sender_id = ((*from).u8[0]);
  dv[this_id][sender_id]=1;
//if new node add to your distance vector (with distance = 1) and broadcast the DV
 
  n = neighbor_find (from);
  if (n == NULL)
    {
      neighbor_add (from, 100, 1);
    }
  else
    {
      neighbor_update (n, 100);
    }
}

static void
node_rm (rimeaddr_t n, struct neighbor *nn)
{
  uint8_t dead_id,i;
  dead_id = n.u8[0];
  PRINTF("node rm\n");
  dv[this_id][dead_id]=MAX_DISTANCE;
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++){
        dv[dead_id][i] = MAX_DISTANCE;
        }
}

static void
recv_SEQ (struct broadcast_conn *broadcast, const rimeaddr_t * from)
{
  uint8_t *size;
  uint8_t i, sz, sender_id;
  sq_msg *sqarray;
  sender_id = ((*from).u8[0]);
  //get pointer to the packet header
  size = packetbuf_dataptr ();
  sqarray = (sq_msg *) (size + 1);
  //get fixed header fields
  sz = *size;
  PRINTF ("SEQfrom(%d)=[", sender_id);
  //update statistics
  recv_size +=
    sizeof (sq_msg) * (sz) + sizeof (uint8_t) +
    soft_broadcast_header_size;
  dv[this_id][sender_id]=1;
  //get distance vector stored in the packet
  for (i = 0; i < sz; i++)
    {
      PRINTF ("%d:%d,", sqarray[i].addr,sqarray[i].sequence);
      if ((sqarray[i].addr == this_id) && (sqarray[i].sequence < seq_num))
            outdated = 1;
    }
  PRINTF ("]\n");
//choose if after receive rebroadcast change or if DV is sent periodically
}

static void
recv_DV (struct broadcast_conn *broadcast, const rimeaddr_t * from)
{
  uint8_t *size;
  uint16_t *sequence;
  dv_msg *dvarray;
  uint8_t i, sz, sender_id;
  sender_id = ((*from).u8[0]);
  //get pointer to the packet header
  sequence = packetbuf_dataptr ();
  size = (uint8_t *) (sequence +1);
  dvarray = (dv_msg *) (size + 1);
  //get fixed header fields
  sz = *size;
  recv_seq_num[sender_id] = *sequence;
  PRINTF ("DVfrom(%d)=[", sender_id);
  //update statistics
  recv_size +=
    sizeof (dv_msg) * (sz) + sizeof (uint8_t) + sizeof(uint16_t) +
    soft_broadcast_header_size;
  dv[this_id][sender_id]=1;
  //before setting new distance vector, set all distances to MAX_DISTANCE (if not in the message, distance is >= MAX_DISTANCE)
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++)
    {
      dv[sender_id][i] = MAX_DISTANCE;
    }
  //get distance vector stored in the packet
  for (i = 0; i < sz; i++)
    {
      PRINTF ("%d:%d:%d,", dvarray[i].addr,dvarray[i].distance,dvarray[i].path);
      if(dvarray[i].path!= this_id) {
        dv[sender_id][dvarray[i].addr] = dvarray[i].distance +1;
        }
      else{
        dv[sender_id][dvarray[i].addr] = MAX_DISTANCE;
        }
    }
  PRINTF ("]\n");
//choose if after receive rebroadcast change or if DV is sent periodically
}

static void
send_SEQ ()
{
   /*
     vector size (8 bit)
     [sq_msg] (24 bit)
   */
  struct neighbor *n;
  uint8_t *size,sz;
  sq_msg *sqarray;
  //update statistics
  sz = list_length (get_neighbors());
  size_beacon+=
    sizeof (sq_msg) * (sz) +
    sizeof (uint8_t)  + soft_broadcast_header_size;
  num_beacon++;
  //start to print distance vector
  PRINTF ("SEQ(%d)=[", rimeaddr_node_addr.u8[0]);
//clear packet buffer and set packet size
  packetbuf_clear ();
  packetbuf_set_datalen (sizeof (sq_msg) * (sz) + sizeof (uint8_t));
  //get pointer to packet and header fields
  size = packetbuf_dataptr ();
  sqarray = (sq_msg *) (size + 1);
  //populate packet header fields (size,sequence number)
  *size = sz;
  //populate packet dynamic part (known distances)
  for (n = list_head(get_neighbors()); n != NULL; n = n->next)
	{
         uint8_t adr = (n->addr).u8[0];
	 sqarray->addr =  adr;
	 sqarray->sequence = recv_seq_num[adr];
	 sqarray++;
	 PRINTF ("%d:%d,",adr, recv_seq_num[adr]);
        }
  PRINTF ("]\n");
  //finally send the packet
  broadcast_send (&bcast2);
}

static void
send_DV ()
{
  /*
     sequence number (16 bit)
     vector size (8 bit)
     [dv_msg] (24 bit)
   */
  uint8_t *size;
  uint16_t *sequence;
  dv_msg *dvarray;
  uint8_t i,j;
 
    
  //update statistics
  size_msg +=
    sizeof (dv_msg) * (reach_num ()) + sizeof (uint16_t) +
    sizeof (uint8_t)  + soft_broadcast_header_size;
  num_msg++;
  //start to print distance vector
  PRINTF ("DV(%d)=[", rimeaddr_node_addr.u8[0]);
//clear packet buffer and set packet size
  packetbuf_clear ();
  packetbuf_set_datalen (sizeof (dv_msg) * (reach_num ()) + sizeof (uint8_t) + sizeof(uint16_t));
  //get pointer to packet and header fields
  sequence = packetbuf_dataptr ();
  size = (uint8_t *) (sequence + 1);
  dvarray = (dv_msg *) (size + 1);
  //populate packet header fields (size,sequence number)
  *sequence = seq_num;
  *size = reach_num ();
  //populate packet dynamic part (known distances)
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++)
    {
      uint8_t min_distance = MAX_DISTANCE;
      uint8_t path = 0;
      //search for shortest distance
      for (j = 1; j <= MAX_REACHABLE_NEIGHBORS; j++){
        if (dv[j][i] < min_distance) {
          min_distance = dv[j][i];
          path = j;
        }
      }
      //if a path exists (distance==MAX_DISTANCE -> infinite distance) add to your vector clock 
      if(min_distance < MAX_DISTANCE){
        dvarray->addr = i;
	dvarray->distance = min_distance;
        dvarray->path = path;
	dvarray++;
        //save in the histiry the last dv sent
        last_dv[i]=min_distance;
	PRINTF ("%d:%d:%d,", i, min_distance,path);
      }
    }
  PRINTF ("]\n");
  //finally send the packet
  broadcast_send (&bcast);
}

void
broadcast_DV(){
 //set timer to periodically send the distance vector
  ctimer_set (&broadcasttimer,
	      CLOCK_SECOND + random_rand () % (4 * CLOCK_SECOND), broadcast_DV,
	      NULL);
  //check if the distance vector is changed (exit if not)
  if (check_changes() == 0) {
      //the distance vector is not changed but some neighbours are not updated. Resend the DV
      if(outdated == 1) {
         send_DV();
         outdated = 0;
         }
    }
  else {
    seq_num ++;
    send_DV();
    }
}

static void
periodic_beacon (void *ptr)
{
  //ctimer_set (&t, CLOCK_SECOND * 10, periodic_beacon, NULL);
  ctimer_set (&t, SEQ_INTERVAL * CLOCK_SECOND + random_rand () % (CLOCK_SECOND), periodic_beacon, NULL);
  //send a beacon with all the sequence numbers received
  send_SEQ();
}

void
adv_send (struct neighbor_discovery_conn *c)
{}

static const struct neighbor_discovery_callbacks neighbor_discovery_callbacks
  = { adv_received, adv_send };
static const struct neighbor_callbacks neighbor_callbacks = { node_rm };
static const struct broadcast_callbacks bcast_callbacks = { recv_DV };
static const struct broadcast_callbacks bcast_callbacks2 = { recv_SEQ };

/*---------------------------------------------------------------------------*/
PROCESS_THREAD (example_polite_process, ev, data)
{
  static struct neighbor_discovery_conn c;
  int i, j;

  PROCESS_EXITHANDLER (neighbor_discovery_close (&c));
  PROCESS_BEGIN ();
  //neighbor discovery init
  neighbor_init_calls (&neighbor_callbacks);
  neighbor_discovery_open (&c, NEIGHBOR_DISCOVERY_CHANNEL, CLOCK_SECOND * 10,
			   CLOCK_SECOND * BEACON_RATE, CLOCK_SECOND * (BEACON_RATE+1),
			   &neighbor_discovery_callbacks);
  neighbor_discovery_start (&c, 100);
  //boradcast init
  broadcast_open (&bcast, BCAST_CHANNEL, &bcast_callbacks);
  broadcast_open (&bcast2, BCAST_CHANNEL2, &bcast_callbacks2);
  //init distance vector table
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++){
      for (j = 1; j <= MAX_REACHABLE_NEIGHBORS; j++){
	  dv[i][j] = MAX_DISTANCE;
	}
    }
  //init history distance vector
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++){
    last_dv[i] = MAX_DISTANCE;
    }
  //init received sequence number vector
  for (i = 1; i <= MAX_REACHABLE_NEIGHBORS; i++){
    recv_seq_num[i] = 0;
    }
  //init self-link cost
  dv[this_id][this_id] = 0;
  //init node id
  this_id = rimeaddr_node_addr.u8[0];
  //set periodic function and periodic DV broadcast
  ctimer_set (&t, SEQ_INTERVAL * CLOCK_SECOND + random_rand () % ( CLOCK_SECOND), periodic_beacon, NULL);
  ctimer_set (&broadcasttimer,
	      CLOCK_SECOND + random_rand () % (5 * CLOCK_SECOND), broadcast_DV,
	      NULL);
  while (1)
    {
      static struct etimer et;
      etimer_set (&et, CLOCK_SECOND * 10);
      PROCESS_WAIT_EVENT_UNTIL (etimer_expired (&et));
      //periodic printout of node status
      /*PRINTF ("%d.%d:sent num:msgs:%ld:ack:%ld:beacons:%ld:total:%ld\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1], soft_num_msg,soft_num_ack,soft_num_beacon,soft_num_msg+soft_num_ack+soft_num_beacon);*/
      printf ("%d.%d:SS:%ld:%ld:%ld:%ld:%ld\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1], size_msg, size_ack, size_beacon, size_msg + size_ack + size_beacon,recv_size);
      printf ("%d.%d:NS:%d\n", rimeaddr_node_addr.u8[0],
	      rimeaddr_node_addr.u8[1], reach_num());

      /*PRINTF ("%d.%d:alive:%d:%d\n", rimeaddr_node_addr.u8[0],
         rimeaddr_node_addr.u8[1], rimeaddr_node_addr.u8[0],
         rimeaddr_node_addr.u8[1]); */
    }
  PROCESS_END ();
}

/*---------------------------------------------------------------------------*/
