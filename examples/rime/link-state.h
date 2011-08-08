#define NEIGHBOR_DISCOVERY_CHANNEL 20
#define BCAST_CHANNEL 10
#define ACK_CHANNEL 15
#define MAX_REACHABLE_NEIGHBORS 30
#define MAX_RETRANSMISSIONS 5
#define MAX_NEIGHBORS 10
#define MAX_MSG_SIZE 20
#define MAX_QUEUE_SIZE 200

struct energy_time {
  long cpu;
  long lpm;
  long transmit;
  long listen;
};

struct paths_list{
  struct paths_list *next;
  rimeaddr_t node_addr;
};

struct reachable_neighbor {
  struct reachable_neighbor *next;
  void *node_paths_list;
  list_t node_paths;
  rimeaddr_t node_addr;
  //path memory
  char paths_list_mem_memb_count[MAX_NEIGHBORS];
  struct paths_list paths_list_mem_memb_mem[MAX_NEIGHBORS];
  struct memb paths_list_mem ;
};

struct reach_msg{
  //operation = a for add or r for remove
  char operation;
  rimeaddr_t node_addr;
  rimeaddr_t path_addr;
};

struct msg_queue{
  struct msg_queue *next;
  struct reach_msg msg;
};

struct tobesent_ack{
  struct tobesent_ack *next;
  rimeaddr_t sender;
};

struct waiting_ack{
  struct waiting_ack *next;
  rimeaddr_t receiver;
};

struct reach_set{
  struct reach_set *next;
  rimeaddr_t node_addr;
};




