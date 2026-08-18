#include "caldrift.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void copy_env(const char *key, const char *fallback, char *dst, size_t n)
{
    const char *v = getenv(key);
    if (v == NULL || *v == '\0')
        v = fallback;
    snprintf(dst, n, "%s", v);
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
        fprintf(stderr, "caldrift: ignoring invalid %s=%s\n", key, v);
        return fallback;
    }
    return parsed;
}

int cd_config_load(cd_config_t *cfg)
{
    copy_env("CD_ARCHIVE_DIR", CD_DEFAULT_ARCHIVE_DIR, cfg->archive_dir, sizeof cfg->archive_dir);
    copy_env("CD_EXCLUDE_SITES", "", cfg->exclude_sites, sizeof cfg->exclude_sites);
    cfg->tolerance_ppm = env_long("CD_TOLERANCE_PPM", 2000);
    cfg->service_interval_days = env_long("CD_SERVICE_INTERVAL_DAYS", 365);
    if (cfg->archive_dir[0] == '\0') {
        fprintf(stderr, "caldrift: CD_ARCHIVE_DIR is empty\n");
        return -1;
    }
    return 0;
}

/* CD_EXCLUDE_SITES carries the sites to leave out of the report as a
 * comma-separated list. Returns 1 when site_id appears in it. */
int cd_config_excluded(const cd_config_t *cfg, const char *site_id)
{
    const char *p = cfg->exclude_sites;
    size_t id_len = strlen(site_id);
    while (*p != '\0') {
        const char *comma = strchr(p, ',');
        size_t len = comma != NULL ? (size_t)(comma - p) : strlen(p);
        if (len == id_len && strncmp(p, site_id, len) == 0)
            return 1;
        if (comma == NULL)
            break;
        p = comma + 1;
    }
    return 0;
}
