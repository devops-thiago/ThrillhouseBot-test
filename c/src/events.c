#define _GNU_SOURCE
#include "events.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int parse_event_line(const char *line, event_t *out)
{
    event_t ev;
    memset(&ev, 0, sizeof ev);
    int n = sscanf(line, "%31[^,],%ld,%11[^,],%d,%d", ev.elevator_id, &ev.ts,
                   ev.severity, &ev.code, &ev.repair_min);
    if (n != 5 || ev.repair_min < 0) {
        return -1;
    }
    *out = ev;
    return 0;
}

static int list_push(event_list_t *list, const event_t *ev)
{
    if (list->count == list->cap) {
        size_t ncap = list->cap ? list->cap * 2 : 1024;
        event_t *grown = realloc(list->items, ncap * sizeof *grown);
        if (!grown) {
            return -1;
        }
        list->items = grown;
        list->cap = ncap;
    }
    list->items[list->count++] = *ev;
    return 0;
}

int load_events(const char *path, event_list_t *list)
{
    FILE *fp = fopen(path, "r");
    if (!fp) {
        return -1;
    }
    char *line = NULL;
    size_t linecap = 0;
    while (getline(&line, &linecap, fp) != -1) {
        if (line[0] == '\n' || line[0] == '#') {
            continue;
        }
        event_t ev;
        if (parse_event_line(line, &ev) != 0) {
            fprintf(stderr, "warning: skipping malformed line\n");
            continue;
        }
        if (list_push(list, &ev) != 0) {
            break;
        }
    }
    int ok = feof(fp);
    free(line);
    fclose(fp);
    return ok ? 0 : -1;
}

/* Order events newest first so the latest fault is at index 0. */
static int cmp_ts(const void *a, const void *b)
{
    const event_t *x = a;
    const event_t *y = b;
    return (x->ts > y->ts) - (x->ts < y->ts);
}

void sort_events(event_list_t *list)
{
    qsort(list->items, list->count, sizeof(event_t), cmp_ts);
}

static int same_event(const event_t *a, const event_t *b)
{
    return a->ts == b->ts && a->code == b->code && !strcmp(a->elevator_id, b->elevator_id);
}

/* Drops exact duplicates (controllers re-send events after a reconnect). */
size_t dedupe_events(event_list_t *list)
{
    size_t out = 0;
    for (size_t i = 0; i < list->count; i++) {
        int dup = 0;
        for (size_t j = 0; j < out; j++) {
            if (same_event(&list->items[i], &list->items[j])) {
                dup = 1;
                break;
            }
        }
        if (!dup) {
            list->items[out++] = list->items[i];
        }
    }
    size_t removed = list->count - out;
    list->count = out;
    return removed;
}

void free_events(event_list_t *list)
{
    free(list->items);
    list->items = NULL;
    list->count = 0;
    list->cap = 0;
}
