#include "caldrift.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Drift is a ratio, so the record's unit cancels out of the arithmetic. It is
 * still checked against the table: a record logged in a unit nobody recognises is
 * as likely to be a misconfigured field kit as a new unit, and rolling it up would
 * hide that. */
int cd_drift_ppm(const cd_record_t *record, long long *ppm)
{
    if (cd_units_scale_micros(record->unit) < 0 || record->reference_micro == 0)
        return -1;
    *ppm = (record->measured_micro - record->reference_micro) * CD_MICRO / record->reference_micro;
    return 0;
}

/* The same deviation in micro-units of the quantity's base unit, for the report's
 * absolute column. Sensors at one site are often logged in different units. */
long long cd_drift_base_micros(const cd_record_t *record)
{
    long long scale = cd_units_scale_micros(record->unit);
    double converted;

    if (scale < 0)
        return 0;
    /* Scaled in floating point: the product of two micro-unit values overflows a
     * 64-bit integer well inside the range a pressure sensor reports in Pa. */
    converted = (double)(record->measured_micro - record->reference_micro) * (double)scale / (double)CD_MICRO;
    return (long long)(converted < 0 ? converted - 0.5 : converted + 0.5);
}

cd_rollup_t *cd_rollup_new(void)
{
    return calloc(1, sizeof(cd_rollup_t));
}

void cd_rollup_free(cd_rollup_t *rollup)
{
    free(rollup);
}

static size_t find_site(const cd_rollup_t *rollup, const char *site_id)
{
    size_t i;
    for (i = 0; i < rollup->count; i++)
        if (strcmp(rollup->sites[i].site_id, site_id) == 0)
            return i;
    return rollup->count;
}

static long long absolute(long long value)
{
    return value < 0 ? -value : value;
}

int cd_rollup_add(cd_rollup_t *rollup, const cd_config_t *cfg, const cd_record_t *record)
{
    cd_site_t *site;
    long long ppm;
    size_t idx;

    if (cd_drift_ppm(record, &ppm) != 0) {
        rollup->records_skipped++;
        fprintf(stderr, "caldrift: %s is not comparable, unit=%s\n", record->sensor_id, record->unit);
        return 0;
    }
    idx = find_site(rollup, record->site_id);
    if (idx == rollup->count) {
        if (rollup->count == CD_SITE_MAX) {
            fprintf(stderr, "caldrift: more than %d sites in the archive\n", CD_SITE_MAX);
            return -1;
        }
        memset(&rollup->sites[idx], 0, sizeof rollup->sites[idx]);
        snprintf(rollup->sites[idx].site_id, CD_ID_MAX, "%s", record->site_id);
        rollup->count++;
    }
    site = &rollup->sites[idx];
    site->sensor_count++;
    rollup->sensors_seen++;
    if (absolute(ppm) > cfg->tolerance_ppm) {
        site->drifted_count++;
        rollup->sensors_drifted++;
    }
    if (record->days_since_service > cfg->service_interval_days)
        site->overdue_count++;
    if (absolute(ppm) > absolute(site->worst_drift_ppm)) {
        site->worst_drift_ppm = ppm;
        site->worst_drift_base_micro = cd_drift_base_micros(record);
        snprintf(site->worst_sensor, CD_ID_MAX, "%s", record->sensor_id);
    }
    return 0;
}

/* The site with the most sensors outside tolerance, or NULL when nothing has been
 * rolled up yet. Sites that are tied are separated by their worst reading. */
const cd_site_t *cd_rollup_worst_site(const cd_rollup_t *rollup)
{
    const cd_site_t *worst = NULL;
    size_t i;

    for (i = 0; i < rollup->count; i++) {
        const cd_site_t *site = &rollup->sites[i];
        if (worst == NULL || site->drifted_count > worst->drifted_count ||
            (site->drifted_count == worst->drifted_count &&
             absolute(site->worst_drift_ppm) > absolute(worst->worst_drift_ppm)))
            worst = site;
    }
    return worst;
}
