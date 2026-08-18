#include "caldrift.h"

#include <stdlib.h>
#include <string.h>

/* Copies the next comma separated field of the row into out and moves cursor past
 * it. Surrounding blanks are dropped: the field kits pad their columns. */
static int next_field(const char **cursor, char *out, size_t n)
{
    const char *start = *cursor, *comma;
    size_t len;

    if (start == NULL)
        return -1;
    comma = strchr(start, ',');
    len = comma != NULL ? (size_t)(comma - start) : strlen(start);
    while (len > 0 && (start[len - 1] == ' ' || start[len - 1] == '\t' ||
                       start[len - 1] == '\r' || start[len - 1] == '\n'))
        len--;
    while (len > 0 && (*start == ' ' || *start == '\t')) {
        start++;
        len--;
    }
    if (len >= n)
        len = n - 1;
    memcpy(out, start, len);
    out[len] = '\0';
    *cursor = comma != NULL ? comma + 1 : NULL;
    return 0;
}

/* Readings are written as decimals, "101.325", and kept as micro-units so the
 * rollup arithmetic stays in integers. */
static int parse_micros(const char *text, long long *out)
{
    char *end = NULL;
    double value;

    if (*text == '\0')
        return -1;
    value = strtod(text, &end);
    if (end == text || *end != '\0')
        return -1;
    *out = (long long)(value * (double)CD_MICRO + (value < 0 ? -0.5 : 0.5));
    return 0;
}

int cd_record_parse(const char *line, cd_record_t *out)
{
    const char *cursor = line;
    char reference[64], measured[64], days[32];
    char *end = NULL;

    memset(out, 0, sizeof *out);
    if (next_field(&cursor, out->sensor_id, sizeof out->sensor_id) != 0 ||
        next_field(&cursor, out->site_id, sizeof out->site_id) != 0 ||
        next_field(&cursor, out->unit, sizeof out->unit) != 0 ||
        next_field(&cursor, reference, sizeof reference) != 0 ||
        next_field(&cursor, measured, sizeof measured) != 0 ||
        next_field(&cursor, days, sizeof days) != 0)
        return -1;
    if (out->sensor_id[0] == '\0' || out->site_id[0] == '\0' || out->unit[0] == '\0')
        return -1;
    if (parse_micros(reference, &out->reference_micro) != 0 ||
        parse_micros(measured, &out->measured_micro) != 0)
        return -1;
    out->days_since_service = strtol(days, &end, 10);
    if (end == days || *end != '\0' || out->days_since_service < 0)
        return -1;
    return 0;
}
