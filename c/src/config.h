#ifndef CONFIG_H
#define CONFIG_H

#include <stddef.h>

#define MAX_STATIONS 16
#define STATION_NAME_LEN 32

typedef struct {
    char stations[MAX_STATIONS][STATION_NAME_LEN];
    size_t station_count;
    long code_ttl;
    int max_codes_per_rider;
    char audit_dir[128];
} config_t;

void config_load(config_t *cfg);
int config_has_station(const config_t *cfg, const char *station);

#endif
