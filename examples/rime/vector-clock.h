#define NEIGHBOR_DISCOVERY_CHANNEL 20
#define BCAST_CHANNEL 10
#define MAX_REACHABLE_NEIGHBORS 40
#define MAX_NEIGHBORS 20




struct timestamp_msg {
  uint8_t addr;
  uint8_t timestamp;
};

typedef struct timestamp_msg ts_msg;




