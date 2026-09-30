#ifndef CONFIG_H
#define CONFIG_H

#include <stddef.h>

#define MAX_IGNORE_CODES 16

typedef struct {
    char log_path[256];
    char export_dir[256];
    int ignore_codes[MAX_IGNORE_CODES];
    size_t n_ignore;
    long window_hours;
} config_t;

void config_load(config_t *cfg);
int config_is_ignored(const config_t *cfg, int code);

#endif
