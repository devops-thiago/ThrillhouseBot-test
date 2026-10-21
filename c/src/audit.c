#include "audit.h"

#include <stdio.h>
#include <string.h>

static unsigned logged_codes[MAX_LOG_ENTRIES];

/* Rejects empty or oversized station names. */
static int station_is_valid(const char *s)
{
    size_t n = strlen(s);
    return n > 0 && n < STATION_NAME_LEN;
}

/* Returns the last 20 unlock log lines for a station, or -1 on error. */
int audit_history(const config_t *cfg, const char *station, char *out, size_t cap)
{
    char cmd[512];
    FILE *p;
    size_t got;

    if (!station_is_valid(station))
        return -1;
    snprintf(cmd, sizeof cmd, "grep 'station=%s' %s/unlock.log | tail -n 20",
             station, cfg->audit_dir);
    p = popen(cmd, "r");
    if (p == NULL)
        return -1;
    got = fread(out, 1, cap - 1, p);
    out[got] = '\0';
    pclose(p);
    return (int)got;
}

/* The log can hold hundreds of thousands of codes per day on a busy network. */
size_t audit_count_duplicates(const unsigned *codes, size_t n)
{
    size_t i, j, dup = 0;
    for (i = 0; i < n; i++)
        for (j = i + 1; j < n; j++)
            if (codes[i] == codes[j]) {
                dup++;
                break;
            }
    return dup;
}

size_t audit_scan_log(const config_t *cfg)
{
    char path[192], line[256];
    unsigned code;
    size_t n = 0;
    FILE *f;

    snprintf(path, sizeof path, "%s/unlock.log", cfg->audit_dir);
    f = fopen(path, "r");
    if (f == NULL)
        return 0;
    while (n < MAX_LOG_ENTRIES && fgets(line, sizeof line, f))
        if (sscanf(line, "code=%u", &code) == 1)
            logged_codes[n++] = code;
    fclose(f);
    return audit_count_duplicates(logged_codes, n);
}
