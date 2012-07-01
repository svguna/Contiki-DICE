/**
 * DICe - Distributed Invariants Checker
 * Monitors a global invariant like 
 * "forall m, n: temperature@m - temperature@n < T"
 * on a wireless sensor network.
 * Copyright (C) 2012 Stefan Guna, svguna@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
