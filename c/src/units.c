#include "caldrift.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CD_UNIT_TABLE_MAX 64

typedef struct {
    char unit[CD_UNIT_MAX];
    long long scale_micros;
} unit_entry_t;

/* Scale of one unit against the base unit of its quantity, in micro-units: Pa for
 * pressure, V for voltage, l/min for flow, degC for temperature. Only ratio units
 * live here — a unit with an offset (degF) cannot be handled by a single factor,
 * and the field kits are configured to log temperature in degC for that reason. */
static unit_entry_t table[CD_UNIT_TABLE_MAX] = {
    { "Pa", 1000000LL },
    { "kPa", 1000000000LL },
    { "bar", 100000000000LL },
    { "psi", 6894757000LL },
    { "V", 1000000LL },
    { "mV", 1000LL },
    { "lpm", 1000000LL },
    { "m3h", 16666667LL },
    { "degC", 1000000LL },
};
static size_t table_count = 9;

static unit_entry_t *find_unit(const char *unit)
{
    size_t i;
    for (i = 0; i < table_count; i++)
        if (strcmp(table[i].unit, unit) == 0)
            return &table[i];
    return NULL;
}

/* Overlays the built-in table with the deployment's units file, one
 * "unit,scale" row per line. A unit already in the table is replaced, so a site
 * that has recalibrated its own reference can ship a corrected factor without a
 * new release. A missing file is not an error: the built-in table stands. */
int cd_units_load(const char *path)
{
    char line[CD_LINE_MAX];
    FILE *fp = fopen(path, "r");
    int loaded = 0;

    if (fp == NULL)
        return 0;
    while (fgets(line, sizeof line, fp) != NULL) {
        char unit[CD_UNIT_MAX];
        char *comma = strchr(line, ',');
        char *end = NULL;
        double scale;
        unit_entry_t *entry;
        size_t len;

        if (line[0] == '#' || comma == NULL)
            continue;
        len = (size_t)(comma - line);
        if (len == 0 || len >= sizeof unit)
            continue;
        memcpy(unit, line, len);
        unit[len] = '\0';
        scale = strtod(comma + 1, &end);
        if (end == comma + 1 || scale <= 0.0) {
            fprintf(stderr, "caldrift: ignoring unusable unit row for %s\n", unit);
            continue;
        }
        entry = find_unit(unit);
        if (entry == NULL) {
            if (table_count == CD_UNIT_TABLE_MAX) {
                fprintf(stderr, "caldrift: unit table is full, %s not loaded\n", unit);
                continue;
            }
            entry = &table[table_count++];
            snprintf(entry->unit, sizeof entry->unit, "%s", unit);
        }
        entry->scale_micros = (long long)(scale * (double)CD_MICRO + 0.5);
        loaded++;
    }
    fclose(fp);
    return loaded;
}

long long cd_units_scale_micros(const char *unit)
{
    const unit_entry_t *entry = find_unit(unit);
    return entry != NULL ? entry->scale_micros : -1;
}
