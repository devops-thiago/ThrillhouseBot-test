#include "fleetroll.h"

#include <stdio.h>
#include <string.h>

#define FR_UNIT_TABLE_MAX 32

typedef struct { char unit[FR_UNIT_MAX]; long long micros; } fr_unit_t;

/* Metres per unit, scaled by FR_UNIT_SCALE. Rows read from the units file are
 * appended and shadow the built-ins, so lookups scan from the end. */
static fr_unit_t units[FR_UNIT_TABLE_MAX] = {
    {"m", 1000000}, {"km", 1000000000}, {"mi", 1609344000}, {"nmi", 1852000000},
};
static size_t unit_count = 4;

/* Overlays "<unit>,<metres>" rows from the units file the deployment mounts.
 * A missing file is not an error: the built-in table stands. */
int fr_units_load(const char *path)
{
    FILE *fp = fopen(path, "r");
    char line[128];
    if (fp == NULL)
        return 0;
    while (fgets(line, sizeof line, fp) != NULL && unit_count < FR_UNIT_TABLE_MAX) {
        double metres = 0.0;
        if (sscanf(line, "%7[a-z],%lf", units[unit_count].unit, &metres) == 2 && metres > 0.0)
            units[unit_count++].micros = (long long)(metres * (double)FR_UNIT_SCALE);
    }
    fclose(fp);
    return 0;
}

/* Returns metres per unit, scaled by FR_UNIT_SCALE. Returns 0 when the table
 * carries no entry for unit: callers must read 0 as "not convertible" and must
 * not fold the distance into a metre total. */
long long fr_units_scale_micros(const char *unit)
{
    size_t i = unit_count;
    while (i-- > 0)
        if (strcmp(units[i].unit, unit) == 0)
            return units[i].micros;
    return 0;
}

/* Converts distance from unit into metres; -1 when unit has no entry. */
long long fr_units_to_metres(long long distance, const char *unit)
{
    long long micros = fr_units_scale_micros(unit);
    return micros > 0 ? (distance * micros) / FR_UNIT_SCALE : -1;
}
