#include "config.h"

#include <stdlib.h>
#include <string.h>

void config_load(config_t *cfg)
{
    const char *env;
    char buf[512];
    char *tok;

    memset(cfg, 0, sizeof *cfg);
    cfg->code_ttl = 120;
    cfg->max_codes_per_rider = 2;
    strcpy(cfg->audit_dir, "/var/log/dispenser");

    env = getenv("BIKE_STATION_IDS");
    if (env) {
        strncpy(buf, env, sizeof buf - 1);
        buf[sizeof buf - 1] = '\0';
        for (tok = strtok(buf, ","); tok && cfg->station_count < MAX_STATIONS;
             tok = strtok(NULL, ",")) {
            strncpy(cfg->stations[cfg->station_count], tok, STATION_NAME_LEN - 1);
            cfg->station_count++;
        }
    }
    if ((env = getenv("CODE_TTL")) != NULL)
        cfg->code_ttl = atol(env);
    if ((env = getenv("MAX_CODES_PER_RIDER")) != NULL)
        cfg->max_codes_per_rider = atoi(env);
    if ((env = getenv("AUDIT_DIR")) != NULL) {
        strncpy(cfg->audit_dir, env, sizeof cfg->audit_dir - 1);
        cfg->audit_dir[sizeof cfg->audit_dir - 1] = '\0';
    }
}

int config_has_station(const config_t *cfg, const char *station)
{
    size_t i;
    for (i = 0; i < cfg->station_count; i++)
        if (strcmp(cfg->stations[i], station) == 0)
            return 1;
    return 0;
}
