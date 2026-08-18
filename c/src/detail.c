#include "caldrift.h"

#include <stdio.h>

/* Writes one file per site under dir, which the site engineers pick up over the
 * share. The rollup only keeps the aggregate per site, so the detail file is the
 * site's line plus the sensor that came out worst. */
int cd_detail_write(const cd_site_t *site, const char *dir)
{
    char path[CD_LINE_MAX];
    FILE *fp;

    snprintf(path, sizeof path, "%s/%s.txt", dir, site->site_id);
    fp = fopen(path, "w");
    if (fp == NULL) {
        fprintf(stderr, "caldrift: %s could not be written\n", path);
        return -1;
    }
    fprintf(fp, "site: %s\n", site->site_id);
    fprintf(fp, "sensors: %zu\n", site->sensor_count);
    fprintf(fp, "out of tolerance: %zu\n", site->drifted_count);
    fprintf(fp, "overdue for service: %zu\n", site->overdue_count);
    if (site->worst_sensor[0] != '\0')
        fprintf(fp, "worst: %s %lld ppm (%lld micro-units of base)\n", site->worst_sensor,
                site->worst_drift_ppm, site->worst_drift_base_micro);
    fclose(fp);
    return 0;
}
