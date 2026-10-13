#pragma once

#include <stddef.h>

#define MAX_SITES 16

typedef struct {
    int locker_count;
    int pin_ttl; /* minutes */
    char sites[MAX_SITES][32];
    size_t site_count;
    const char *carrier_url;
    const char *carrier_token;
} locker_config;

/* Reads settings from the environment. Returns -1 if a required one is missing. */
int config_load(locker_config *cfg);
