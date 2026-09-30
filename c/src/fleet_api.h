#ifndef FLEET_API_H
#define FLEET_API_H

#include <stddef.h>

#include "events.h"

#define FLEET_PAGE_SIZE 50

typedef struct {
    char ids[FLEET_PAGE_SIZE][MAX_ID_LEN];
    size_t count;
    int has_more;      /* non-zero when another page follows */
    long next_cursor;  /* pass to the next fetch when has_more is set */
} fleet_page_t;

typedef int (*fleet_fetch_fn)(long cursor, fleet_page_t *out);

/* Reads one page of the fleet inventory snapshot. Returns 0 on success. */
int fleet_api_fetch_page(long cursor, fleet_page_t *out);

/* Number of elevators registered in the fleet inventory. */
size_t fleet_inventory_size(fleet_fetch_fn fetch);

#endif
