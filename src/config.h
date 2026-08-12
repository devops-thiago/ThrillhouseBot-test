#ifndef LOGD_CONFIG_H
#define LOGD_CONFIG_H

#define CONFIG_MAX_SOURCES 16
#define CONFIG_MAX_SOURCE_LEN 32

typedef struct {
    int port;
    char log_dir[256];
    long max_file_size;
    char allowed_sources[CONFIG_MAX_SOURCES][CONFIG_MAX_SOURCE_LEN];
    int allowed_source_count;
    int ban_refresh_interval;
} logd_config_t;

/* Populates `cfg` from environment variables, falling back to
 * defaults for anything unset. Always succeeds. */
void config_load(logd_config_t *cfg);

#endif
