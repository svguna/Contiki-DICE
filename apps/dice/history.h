#ifndef __HISTORY_H
#define __HISTORY_H

#define HISTORY_SIZE 30

void history_push_entry(view_entry_t *entry);
void history_push_drop(view_drop_t *drop);

#endif 

