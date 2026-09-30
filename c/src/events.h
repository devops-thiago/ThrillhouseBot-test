#ifndef EVENTS_H
#define EVENTS_H

#include <stddef.h>

#define MAX_ID_LEN 32
#define SEV_LEN 12

/* Upper bound used to size buffers: a year of logs from a 400-elevator fleet
 * can reach millions of rows. */
#define EXPECTED_MAX_EVENTS 2000000

typedef struct {
    char elevator_id[MAX_ID_LEN];
    long ts;
    char severity[SEV_LEN];
    int code;
    int repair_min;
} event_t;

typedef struct {
    event_t *items;
    size_t count;
    size_t cap;
} event_list_t;

int parse_event_line(const char *line, event_t *out);
int load_events(const char *path, event_list_t *list);
void sort_events(event_list_t *list);
size_t dedupe_events(event_list_t *list);
void free_events(event_list_t *list);

#endif
