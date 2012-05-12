#ifndef __GROUP_H
#define __GROUP_H

#include "net/rime.h"

#define MAX_NODES 25
#define GROUP_CHANNEL 130

#define CLOCK_TICK_PERIOD (1 * CLOCK_SECOND)

int groupmon_init(uint32_t newNeighborLatency, uint32_t missingNeighborLatency);
int groupmon_stop();
int groupmon_reset();
void groupmon_forceupdate(rimeaddr_t *addr);
int groupmon_isalive(rimeaddr_t *addr);

void groupmon_evict(rimeaddr_t *addr); // callback
#endif
