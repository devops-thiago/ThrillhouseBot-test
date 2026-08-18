#include "caldrift.h"

#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv)
{
    const char *units_path = argc > 1 ? argv[1] : CD_DEFAULT_UNITS_PATH;
    cd_record_t *records = NULL;
    cd_config_t cfg;
    cd_rollup_t *rollup;
    size_t n = 0, i;
    int rc = 0;

    if (cd_config_load(&cfg) != 0)
        return 2;
    cd_units_load(units_path);
    if (cd_archive_walk(&cfg, &records, &n) != 0) {
        fprintf(stderr, "caldrift: unable to read the calibration archive at %s\n", cfg.archive_dir);
        return 1;
    }
    rollup = cd_rollup_new();
    if (rollup == NULL) {
        free(records);
        return 1;
    }
    for (i = 0; i < n; i++) {
        if (cd_config_excluded(&cfg, records[i].site_id))
            continue;
        if (cd_rollup_add(rollup, &cfg, &records[i]) != 0) {
            rc = 1;
            break;
        }
    }
    free(records);
    for (i = 0; rc == 0 && i < rollup->count; i++)
        cd_detail_write(&rollup->sites[i], CD_DEFAULT_DETAIL_DIR);
    if (rc == 0 && cd_report_write(&cfg, rollup, CD_DEFAULT_REPORT_PATH) != 0)
        rc = 1;
    printf("caldrift: %zu sites, %zu sensors, %zu outside tolerance, %zu records skipped\n",
           rollup->count, rollup->sensors_seen, rollup->sensors_drifted, rollup->records_skipped);
    cd_rollup_free(rollup);
    return rc;
}
