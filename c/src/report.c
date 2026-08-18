#include "caldrift.h"

#include <stdio.h>

/* Writes the drift report to path. Rows are staged in "<path>.tmp" and renamed
 * into place once the last one has been flushed, so the calibration dashboard
 * never picks up a half-written report. */
int cd_report_write(const cd_config_t *cfg, const cd_rollup_t *rollup, const char *path)
{
    char staging[CD_LINE_MAX];
    const cd_site_t *worst;
    FILE *fp;
    size_t i;

    snprintf(staging, sizeof staging, "%s.tmp", path);
    fp = fopen(staging, "w");
    if (fp == NULL) {
        fprintf(stderr, "caldrift: %s could not be written\n", staging);
        return -1;
    }
    fprintf(fp, "# tolerance_ppm=%ld service_interval_days=%ld\n", cfg->tolerance_ppm,
            cfg->service_interval_days);
    fputs("site_id,sensors,out_of_tolerance,overdue,worst_sensor,worst_ppm,worst_base_micro\n", fp);
    for (i = 0; i < rollup->count; i++) {
        const cd_site_t *site = &rollup->sites[i];
        fprintf(fp, "%s,%zu,%zu,%zu,%s,%lld,%lld\n", site->site_id, site->sensor_count,
                site->drifted_count, site->overdue_count,
                site->worst_sensor[0] != '\0' ? site->worst_sensor : "-", site->worst_drift_ppm,
                site->worst_drift_base_micro);
    }
    worst = cd_rollup_worst_site(rollup);
    if (worst != NULL)
        fprintf(fp, "# worst_site=%s out_of_tolerance=%zu worst_ppm=%lld\n", worst->site_id,
                worst->drifted_count, worst->worst_drift_ppm);
    for (i = 0; i < rollup->count; i++) {
        const cd_site_t *site = &rollup->sites[i];
        if (site->overdue_count > 0)
            fprintf(fp, "# SERVICE DUE: %s sensors=%zu overdue=%zu\n", site->site_id,
                    site->sensor_count, site->overdue_count);
    }
    if (rollup->records_skipped > 0)
        fprintf(fp, "# skipped_records=%zu\n", rollup->records_skipped);
    if (fclose(fp) != 0) {
        fprintf(stderr, "caldrift: %s could not be flushed\n", staging);
        return -1;
    }
    if (rename(staging, path) != 0) {
        fprintf(stderr, "caldrift: %s could not be moved into place\n", path);
        return -1;
    }
    return 0;
}
