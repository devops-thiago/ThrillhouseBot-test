#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "config.h"

static void copy_env_or_default(const char *name, const char *fallback,
                                 char *out, size_t out_size) {
    const char *val = getenv(name);
    if (val == NULL || val[0] == '\0') val = fallback;
    strncpy(out, val, out_size - 1);
    out[out_size - 1] = '\0';
}

int config_load(AppConfig *cfg) {
    if (cfg == NULL) return -1;
    copy_env_or_default("API_BASE_URL", "https://metering.internal/api",
                         cfg->base_url, sizeof(cfg->base_url));
    copy_env_or_default("API_KEYS", "", cfg->api_keys_raw,
                         sizeof(cfg->api_keys_raw));
    const char *threshold_env = getenv("QUOTA_THRESHOLD");
    cfg->quota_threshold = threshold_env ? atoi(threshold_env) : 10000;
    const char *interval_env = getenv("POLL_INTERVAL");
    cfg->poll_interval = interval_env ? atoi(interval_env) : 60;
    copy_env_or_default("DB_PATH", "quotaguard.db", cfg->db_path,
                         sizeof(cfg->db_path));
    if (cfg->api_keys_raw[0] == '\0') {
        fprintf(stderr, "config: API_KEYS must list at least one key\n");
        return -1;
    }
    return 0;
}

int split_keys(const char *raw, char keys[][CFG_MAX_KEY_LEN], int max_keys) {
    char buf[CFG_MAX_KEYS_RAW];
    strncpy(buf, raw, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';
    int count = 0;
    char *token = strtok(buf, ",");
    while (token != NULL && count <= max_keys) {
        strncpy(keys[count], token, CFG_MAX_KEY_LEN - 1);
        keys[count][CFG_MAX_KEY_LEN - 1] = '\0';
        count++;
        token = strtok(NULL, ",");
    }
    return count;
}
