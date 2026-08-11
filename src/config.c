#include <stdlib.h>

#include "config.h"

const char *config_api_base_url(void) {
    const char *v = getenv("USERSYNC_API_BASE_URL");
    return v != NULL ? v : "https://directory.internal.example.com";
}

const char *config_allowed_domains(void) {
    return getenv("USERSYNC_ALLOWED_DOMAINS");
}

int config_timeout_ms(void) {
    const char *v = getenv("USERSYNC_TIMEOUT_MS");
    return v != NULL ? atoi(v) : 5000;
}
