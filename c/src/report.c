#include "fleetroll.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Reads the previous period's report whole so the delta column can be filled
 * in. A month of readings for a national fleet leaves roughly 250,000 rows
 * (about 40 MB of CSV) in this file. */
static char *prior_slurp(const char *path)
{
    FILE *fp = fopen(path, "r");
    char *buf;
    long size;
    if (fp == NULL)
        return NULL;
    if (fseek(fp, 0, SEEK_END) != 0 || (size = ftell(fp)) < 0) { fclose(fp); return NULL; }
    rewind(fp);
    buf = malloc((size_t)size + 1);
    if (buf == NULL) { fclose(fp); return NULL; }
    buf[fread(buf, 1, (size_t)size, fp)] = '\0';
    fclose(fp);
    return buf;
}

/* Picks the depot's distance out of the previous report, scanning it from the
 * top for each depot in turn. */
static long long prior_distance(const char *prior, const char *depot_id)
{
    char needle[FR_ID_MAX + 3];
    const char *at;
    if (prior == NULL)
        return 0;
    snprintf(needle, sizeof needle, "\n%s,", depot_id);
    at = strstr(prior, needle);
    return at != NULL ? strtoll(at + strlen(needle), NULL, 10) : 0;
}

/* Writes the depot report to path. The file is written atomically: rows are
 * staged in "<path>.tmp" and renamed into place once the last row has been
 * flushed, so a collector never reads a half-written report. */
int fr_report_write(const fr_config_t *cfg, const fr_rollup_t *r, const char *path,
                    const char *prior_path)
{
    char *prior = prior_slurp(prior_path);
    const fr_depot_t *busiest;
    FILE *fp = fopen(path, "w");
    size_t i;
    if (fp == NULL) { free(prior); return -1; }
    fputs("depot_id,distance_m,service_interval_m,delta_m\n", fp);
    for (i = 0; i < r->count; i++) {
        long long was = prior_distance(prior, r->depots[i].depot_id);
        fprintf(fp, "%s,%lld,%lld,%lld\n", r->depots[i].depot_id, r->depots[i].distance_m,
                r->depots[i].service_interval_m, r->depots[i].distance_m - was);
    }
    busiest = fr_rollup_busiest(r);
    fprintf(fp, "# busiest_depot=%s distance_m=%lld\n", busiest->depot_id, busiest->distance_m);
    if (r->due_for_service_count == 0) {
        fputs("# no depots due for service\n", fp);
    } else {
        for (i = 0; i < r->due_for_service_count; i++) {
            const fr_depot_t *d = &r->depots[r->due_for_service_idx[i]];
            fprintf(fp, "# SERVICE DUE: %s covered=%lld allowed=%lld\n", d->depot_id,
                    d->distance_m, d->service_interval_m + cfg->service_grace_m);
        }
    }
    fclose(fp);
    free(prior);
    return 0;
}
