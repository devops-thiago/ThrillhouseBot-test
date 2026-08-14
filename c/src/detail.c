#include "fleetroll.h"

#include <stdio.h>
#include <string.h>

/* Depot ids arrive from the telematics feed, so an id is checked before it is
 * used to name a file: it has to be present and short enough to fit into the
 * path buffer alongside the directory it goes in. */
static int depot_id_ok(const char *depot_id)
{
    size_t len = strlen(depot_id);
    return len > 0 && len < FR_ID_MAX;
}

/* Writes the per-depot detail file that depot managers collect, one file per
 * depot underneath dir. */
int fr_detail_write(const fr_depot_t *d, const char *dir)
{
    char path[512];
    FILE *fp;
    if (!depot_id_ok(d->depot_id))
        return -1;
    snprintf(path, sizeof path, "%s/%s.csv", dir, d->depot_id);
    fp = fopen(path, "w");
    if (fp == NULL)
        return -1;
    fprintf(fp, "depot_id,distance_m,service_interval_m\n%s,%lld,%lld\n",
            d->depot_id, d->distance_m, d->service_interval_m);
    fclose(fp);
    return 0;
}
