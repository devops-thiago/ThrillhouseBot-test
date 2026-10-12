#include "meterval.h"

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>

/* Only [A-Za-z0-9_-] may be interpolated into the archive shell command. */
static int shell_safe(const char *s)
{
    for (; *s != '\0'; s++)
        if (!isalnum((unsigned char)*s) && *s != '-' && *s != '_')
            return 0;
    return 1;
}

int archive_rejected(const struct meter_config *cfg, const char *zone,
                     const struct batch_result *res)
{
    if (!shell_safe(zone))
        return -1;
    char cmd[1024];
    for (size_t i = 0; i < res->rejected_count; i++) {
        const struct reading *r = &res->rejected_readings[i];
        snprintf(cmd, sizeof cmd, "mkdir -p '%s/%s' && echo '%ld,%.1f,%s' >> '%s/%s/%s.csv'",
                 cfg->archive_dir, zone, r->timestamp, r->litres,
                 reject_reason_name(res->reasons[i]), cfg->archive_dir, zone, r->serial);
        if (system(cmd) != 0)
            return -1;
    }
    return 0;
}
