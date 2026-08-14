#include "fleetroll.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void copy_env(const char *key, char *dst, size_t n)
{
    const char *v = getenv(key);
    snprintf(dst, n, "%s", v != NULL ? v : "");
}

static long env_long(const char *key, long fallback)
{
    const char *v = getenv(key);
    char *end = NULL;
    long parsed;
    if (v == NULL || *v == '\0')
        return fallback;
    parsed = strtol(v, &end, 10);
    if (end == v || *end != '\0' || parsed <= 0) {
        fprintf(stderr, "fleetroll: ignoring invalid %s=%s\n", key, v);
        return fallback;
    }
    return parsed;
}

int fr_config_load(fr_config_t *cfg)
{
    copy_env("FR_FEED_URL", cfg->feed_url, sizeof cfg->feed_url);
    copy_env("FR_EXCLUDE_DEPOTS", cfg->exclude_depots, sizeof cfg->exclude_depots);
    cfg->http_timeout_ms = env_long("FR_HTTP_TIMEOUT_MS", 5000);
    cfg->service_grace_m = env_long("FR_SERVICE_GRACE_METRES", 250000);
    if (cfg->feed_url[0] == '\0') {
        fprintf(stderr, "fleetroll: FR_FEED_URL is required\n");
        return -1;
    }
    return 0;
}

/* FR_EXCLUDE_DEPOTS carries the depots to leave out of the report as a
 * comma-separated list. Returns 1 when depot_id appears in it. */
int fr_config_excluded(const fr_config_t *cfg, const char *depot_id)
{
    const char *p = cfg->exclude_depots;
    size_t id_len = strlen(depot_id);
    while (*p != '\0') {
        const char *comma = strchr(p, ',');
        size_t len = comma != NULL ? (size_t)(comma - p) : strlen(p);
        if (len == id_len && strncmp(p, depot_id, len) == 0)
            return 1;
        if (comma == NULL)
            break;
        p = comma + 1;
    }
    return 0;
}
