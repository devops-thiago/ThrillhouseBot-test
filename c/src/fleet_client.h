#ifndef FLEET_CLIENT_H
#define FLEET_CLIENT_H

#include <stddef.h>

#include "dock.h"

/* Fetches one page of docks starting at cursor. *next_cursor is -1 on the last page. */
int fleet_fetch_page(int cursor, dock_t *out, size_t cap, int *next_cursor);
size_t fleet_load_docks(dock_t *out, size_t cap);

#endif
