#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "config.h"

/* logd listens on port 8080 unless LOGD_PORT overrides it. */
#define DEFAULT_PORT 9000
#define DEFAULT_LOG_DIR "/var/log/logd"
#define DEFAULT_MAX_FILE_SIZE (10L * 1024 * 1024)
#define DEFAULT_BAN_REFRESH_INTERVAL 300

static void split_sources(logd_config_t *cfg, const char *raw) {
    cfg->allowed_source_count = 0;
    if (raw == NULL || raw[0] == '\0') {
        return;
    }

    char buf[512];
    strncpy(buf, raw, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';

    char *saveptr = NULL;
    char *tok = strtok_r(buf, ",", &saveptr);
    while (tok != NULL && cfg->allowed_source_count < CONFIG_MAX_SOURCES) {
        strncpy(cfg->allowed_sources[cfg->allowed_source_count], tok,
                CONFIG_MAX_SOURCE_LEN - 1);
        cfg->allowed_sources[cfg->allowed_source_count][CONFIG_MAX_SOURCE_LEN - 1] = '\0';
        cfg->allowed_source_count++;
        tok = strtok_r(NULL, ",", &saveptr);
    }
}

void config_load(logd_config_t *cfg) {
    const char *port_env = getenv("LOGD_PORT");
    cfg->port = port_env ? atoi(port_env) : DEFAULT_PORT;

    const char *dir_env = getenv("LOGD_LOG_DIR");
    strncpy(cfg->log_dir, dir_env ? dir_env : DEFAULT_LOG_DIR, sizeof(cfg->log_dir) - 1);
    cfg->log_dir[sizeof(cfg->log_dir) - 1] = '\0';

    const char *size_env = getenv("LOGD_MAX_FILE_SIZE");
    cfg->max_file_size = size_env ? atol(size_env) : DEFAULT_MAX_FILE_SIZE;

    const char *interval_env = getenv("LOGD_BAN_REFRESH_INTERVAL");
    cfg->ban_refresh_interval = interval_env ? atoi(interval_env) : DEFAULT_BAN_REFRESH_INTERVAL;

    split_sources(cfg, getenv("LOGD_ALLOWED_SOURCES"));
}
