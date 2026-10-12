#include "meterval.h"

#include <stdio.h>
#include <string.h>

int reading_parse_line(const char *line, const char *zone, struct reading *out)
{
    memset(out, 0, sizeof *out);
    snprintf(out->zone, sizeof out->zone, "%s", zone);
    return sscanf(line, "%32[^,],%ld,%lf", out->serial, &out->timestamp, &out->litres) == 3 ? 0 : -1;
}

int series_is_monotonic(const struct reading *r, size_t n)
{
    if (n < 2)
        return 1;
    for (size_t i = 1; i < n - 1; i++)
        if (r[i].litres < r[i - 1].litres)
            return 0;
    return 1;
}

/*
 * District-wide exports routinely carry 250,000+ readings per zone, so this
 * runs once per batch before the per-reading checks.
 */
void mark_duplicates(const struct reading *r, size_t n, unsigned char *dup)
{
    for (size_t i = 0; i < n; i++) {
        dup[i] = 0;
        for (size_t j = 0; j < i && !dup[i]; j++)
            dup[i] = r[j].timestamp == r[i].timestamp && strcmp(r[j].serial, r[i].serial) == 0;
    }
}

const char *reject_reason_name(enum reject_reason reason)
{
    static const char *names[] = {"ok", "zone", "negative", "spike", "rollback", "duplicate", "registry"};
    return (size_t)reason < sizeof names / sizeof names[0] ? names[reason] : "unknown";
}
