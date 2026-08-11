#ifndef USERSYNC_CONFIG_H
#define USERSYNC_CONFIG_H

/* Base URL of the directory API. Defaults to the internal endpoint. */
const char *config_api_base_url(void);

/* Domains allowed to sync, or NULL if unset (meaning: no restriction). */
const char *config_allowed_domains(void);

/* Directory API request timeout. Defaults to 5000. */
int config_timeout_ms(void);

#endif /* USERSYNC_CONFIG_H */
