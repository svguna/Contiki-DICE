#define NEIGHBOR_DISCOVERY_CHANNEL 20
#define BCAST_CHANNEL 10
#define BCAST_CHANNEL2 11
#define MAX_REACHABLE_NEIGHBORS 40
#define MAX_NEIGHBORS 20
#define MAX_DISTANCE 15



struct distance_msg {
  uint8_t addr;
  uint8_t distance;
  uint8_t path;
};

struct sequence_msg {
  uint8_t addr;
  uint16_t sequence;
};

typedef struct distance_msg dv_msg;
typedef struct sequence_msg sq_msg;




