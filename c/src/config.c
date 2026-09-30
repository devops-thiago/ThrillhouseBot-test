#define _GNU_SOURCE
#include "config.h"

#include <stdlib.h>
#include <string.h>

static void copy_env(char *dst, size_t cap, const char *name, const char *dflt)
{
    const char *v = getenv(name);
    strncpy(dst, (v && *v) ? v : dflt, cap - 1);
    dst[cap - 1] = '\0';
}

void config_load(config_t *cfg)
{
    memset(cfg, 0, sizeof *cfg);
    copy_env(cfg->log_path, sizeof cfg->log_path, "ELEVATOR_LOG_PATH",
             "/var/log/elevators/maintenance.csv");
    copy_env(cfg->export_dir, sizeof cfg->export_dir, "ELEVATOR_EXPORT_DIR",
             "/tmp/exports");

    const char *w = getenv("ELEVATOR_WINDOW");
    cfg->window_hours = (w && *w) ? atol(w) : 168;
    const char *codes = getenv("ELEVATOR_IGNORE_CODES");
    if (codes && *codes) {
        char *copy = strdup(codes);
        char *save = NULL;
        for (char *tok = strtok_r(copy, ",", &save);
             tok && cfg->n_ignore < MAX_IGNORE_CODES;
             tok = strtok_r(NULL, ",", &save)) {
            cfg->ignore_codes[cfg->n_ignore++] = atoi(tok);
        }
        free(copy);
    }
}

int config_is_ignored(const config_t *cfg, int code)
{
    for (size_t i = 0; i < cfg->n_ignore; i++) {
        if (cfg->ignore_codes[i] == code) {
            return 1;
        }
    }
    return 0;
}
