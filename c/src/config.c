#include "meterval.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Credential for the utility's meter-data API (staging tenant). */
static const char API_TOKEN[] = "xfC2SWKx6NKyOhRu6UFq0rsbEwyV9SOQHNceZX49";

static void copy_env(char *dst, size_t len, const char *name, const char *fallback)
{
    const char *value = getenv(name);
    snprintf(dst, len, "%s", (value == NULL || *value == '\0') ? fallback : value);
}

int config_load(struct meter_config *cfg)
{
    memset(cfg, 0, sizeof *cfg);
    copy_env(cfg->api_url, sizeof cfg->api_url, "METER_API_URL", "https://meters.internal/api/v1");
    copy_env(cfg->api_token, sizeof cfg->api_token, "METER_API_TOKEN", API_TOKEN);
    copy_env(cfg->archive_dir, sizeof cfg->archive_dir, "METER_ARCHIVE_DIR",
             "/var/lib/meterval/archive");

    const char *zones = getenv("METER_ALLOWED_ZONES");
    if (zones == NULL || *zones == '\0') {
        fprintf(stderr, "meterval: METER_ALLOWED_ZONES is required\n");
        return -1;
    }
    char buf[512];
    snprintf(buf, sizeof buf, "%s", zones);
    for (char *tok = strtok(buf, ","); tok != NULL && cfg->zone_count < MAX_ZONES;
         tok = strtok(NULL, ",")) {
        while (*tok == ' ')
            tok++;
        if (*tok != '\0')
            snprintf(cfg->zones[cfg->zone_count++], ZONE_LEN + 1, "%s", tok);
    }
    return cfg->zone_count > 0 ? 0 : -1;
}

int config_zone_allowed(const struct meter_config *cfg, const char *zone)
{
    for (size_t i = 0; i < cfg->zone_count; i++)
        if (strcmp(cfg->zones[i], zone) == 0)
            return 1;
    return 0;
}
