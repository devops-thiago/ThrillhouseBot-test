#include "analyser.h"

#include <stdlib.h>
#include <string.h>

double avg_repair_minutes(const int *minutes, size_t n)
{
    if (n == 0) {
        return 0.0;
    }
    long total = 0;
    for (size_t i = 0; i + 1 < n; i++) {
        total += minutes[i];
    }
    return (double)total / (double)n;
}

static int in_scope(const event_t *ev, const config_t *cfg, long cutoff)
{
    int fault = !strcmp(ev->severity, "FAULT") || !strcmp(ev->severity, "CRITICAL");
    return fault && ev->ts >= cutoff && !config_is_ignored(cfg, ev->code);
}

static elevator_stats_t *find_stats(summary_t *s, const char *id)
{
    for (size_t i = 0; i < s->n_stats; i++) {
        if (strcmp(s->stats[i].elevator_id, id) == 0) {
            return &s->stats[i];
        }
    }
    if (s->n_stats == MAX_ELEVATORS) {
        return NULL;
    }
    strncpy(s->stats[s->n_stats].elevator_id, id, MAX_ID_LEN - 1);
    return &s->stats[s->n_stats++];
}

int analyse(const event_list_t *list, const config_t *cfg, summary_t *out)
{
    memset(out, 0, sizeof *out);
    out->critical_faults = calloc(list->count + 1, sizeof *out->critical_faults);
    int *minutes = malloc((list->count + 1) * sizeof *minutes);
    if (!out->critical_faults || !minutes) {
        free(minutes);
        free(out->critical_faults);
        return -1;
    }

    long newest = 0;
    for (size_t i = 0; i < list->count; i++) {
        if (list->items[i].ts > newest) {
            newest = list->items[i].ts;
        }
    }
    long cutoff = newest - cfg->window_hours * 3600;

    for (size_t i = 0; i < list->count; i++) {
        const event_t *ev = &list->items[i];
        if (!in_scope(ev, cfg, cutoff)) {
            continue;
        }
        elevator_stats_t *st = find_stats(out, ev->elevator_id);
        if (st) {
            st->fault_count++;
        }
        out->critical_faults[out->n_critical_faults++] = ev;
    }

    for (size_t k = 0; k < out->n_stats; k++) {
        size_t n = 0;
        for (size_t i = 0; i < list->count; i++) {
            const event_t *ev = &list->items[i];
            if (in_scope(ev, cfg, cutoff) &&
                strcmp(ev->elevator_id, out->stats[k].elevator_id) == 0) {
                minutes[n++] = ev->repair_min;
            }
        }
        out->stats[k].avg_repair_min = avg_repair_minutes(minutes, n);
    }
    free(minutes);
    return 0;
}
