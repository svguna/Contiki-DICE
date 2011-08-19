#ifndef __DRICKLE_H
#define __DRICKLE_H

#define DRICKLE_CHANNEL 129

#define TRICKLE_LOW (CLOCK_SECOND / 5)
#define TRICKLE_HIGH (CLOCK_SECOND * 4)
#define TRICKLE_REDUNDANCY 5

void drickle_init();
void drickle_reset();

#endif
