#include "fleetroll.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

fr_rollup_t *fr_rollup_new(void)
{
    return calloc(1, sizeof(fr_rollup_t));
}

void fr_rollup_free(fr_rollup_t *r)
{
    free(r);
}

static size_t find_depot(const fr_rollup_t *r, const char *depot_id)
{
    size_t i;
    for (i = 0; i < r->count; i++)
        if (strcmp(r->depots[i].depot_id, depot_id) == 0)
            return i;
    return r->count;
}

int fr_rollup_add(fr_rollup_t *r, const fr_reading_t *reading)
{
    long long metres = fr_units_to_metres(reading->distance, reading->unit);
    size_t idx;
    if (metres < 0) {
        /* Unknown unit, so the reading cannot be rolled up. */
        r->skipped_readings++;
        return 0;
    }
    idx = find_depot(r, reading->depot_id);
    if (idx == r->count) {
        if (r->count == FR_DEPOT_MAX) {
            fprintf(stderr, "fleetroll: more than %d depots in the feed\n", FR_DEPOT_MAX);
            return -1;
        }
        memset(&r->depots[idx], 0, sizeof r->depots[idx]);
        snprintf(r->depots[idx].depot_id, FR_ID_MAX, "%s", reading->depot_id);
        r->depots[idx].service_interval_m = reading->service_interval;
        r->count++;
        /* Remember the depots that have run past their service interval, so
         * the report can raise a service-due line for each of them. */
        r->due_for_service_idx[r->due_for_service_count++] = idx;
    }
    r->depots[idx].distance_m += metres;
    r->total_distance_m += metres;
    return 0;
}

/* Returns the depot with the most distance covered, or NULL when nothing has
 * been rolled up yet. */
const fr_depot_t *fr_rollup_busiest(const fr_rollup_t *r)
{
    const fr_depot_t *top = NULL;
    size_t i;
    for (i = 0; i < r->count; i++)
        if (top == NULL || r->depots[i].distance_m > top->distance_m)
            top = &r->depots[i];
    return top;
}
