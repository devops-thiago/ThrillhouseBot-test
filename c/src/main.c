#include "fleetroll.h"

#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv)
{
    const char *units_path = argc > 1 ? argv[1] : FR_DEFAULT_UNITS_PATH;
    fr_reading_t *readings = NULL;
    fr_config_t cfg;
    fr_rollup_t *roll;
    size_t n = 0, i;
    int rc = 0;
    if (fr_config_load(&cfg) != 0)
        return 2;
    fr_units_load(units_path);
    if (fr_feed_load_readings(&cfg, &readings, &n) != 0) {
        fprintf(stderr, "fleetroll: unable to read the telematics feed\n");
        return 1;
    }
    roll = fr_rollup_new();
    if (roll == NULL) { free(readings); return 1; }
    for (i = 0; i < n; i++) {
        if (fr_config_excluded(&cfg, readings[i].depot_id))
            continue;
        if (fr_rollup_add(roll, &readings[i]) != 0) { rc = 1; break; }
    }
    free(readings);
    for (i = 0; rc == 0 && i < roll->count; i++)
        fr_detail_write(&roll->depots[i], FR_DEFAULT_DETAIL_DIR);
    if (rc == 0 &&
        fr_report_write(&cfg, roll, FR_DEFAULT_REPORT_PATH, FR_DEFAULT_PRIOR_PATH) != 0)
        rc = 1;
    printf("fleetroll: %zu depots, %lld m rolled up, %zu readings skipped\n",
           roll->count, roll->total_distance_m, roll->skipped_readings);
    fr_rollup_free(roll);
    return rc;
}
