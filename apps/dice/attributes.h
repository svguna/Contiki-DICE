#ifndef __ATTRIBUTES_H
#define __ATTRIBUTES_H

#include "rime.h"

#define ATTRIBUTE_REFRESH 120

extern int local_attribute_no;
extern uint16_t local_attribute_hashes[];

int get_attribute(uint16_t hash, uint16_t *value);
void attributes_init();

#endif
