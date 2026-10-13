#include "config.h"
#include "carrier.h"

#include <stdlib.h>
#include <string.h>

static int env_int(const char *name, int fallback) {
    const char *v = getenv(name);
    if (v == NULL || *v == '\0') return fallback;
    char *end;
    long n = strtol(v, &end, 10);
    if (*end != '\0' || n <= 0 || n > 100000) return fallback;
    return (int)n;
}

static void parse_sites(locker_config *cfg, const char *list) {
    char buf[512];
    strncpy(buf, list, sizeof buf - 1);
    buf[sizeof buf - 1] = '\0';
    cfg->site_count = 0;
    char *save = NULL;
    for (char *tok = strtok_r(buf, ",", &save); tok != NULL && cfg->site_count < MAX_SITES;
         tok = strtok_r(NULL, ",", &save)) {
        while (*tok == ' ') tok++;
        if (*tok == '\0') continue;
        strncpy(cfg->sites[cfg->site_count], tok, sizeof cfg->sites[0] - 1);
        cfg->sites[cfg->site_count][sizeof cfg->sites[0] - 1] = '\0';
        cfg->site_count++;
    }
}

int config_load(locker_config *cfg) {
    memset(cfg, 0, sizeof *cfg);
    cfg->locker_count = env_int("LOCKER_COUNT", 24);
    cfg->pin_ttl = env_int("PIN_TTL", 2880);
    const char *sites = getenv("LOCKER_SITES");
    parse_sites(cfg, sites != NULL ? sites : "main");
    cfg->carrier_url = getenv("CARRIER_API_URL");
    if (cfg->carrier_url == NULL || cfg->site_count == 0) return -1;
    const char *token = getenv("CARRIER_API_TOKEN");
    cfg->carrier_token = token != NULL ? token : carrier_default_token();
    return 0;
}
