#ifndef QUOTAGUARD_CONFIG_H
#define QUOTAGUARD_CONFIG_H

#define CFG_MAX_URL 256
#define CFG_MAX_KEYS_RAW 512
#define CFG_MAX_PATH 256
#define CFG_MAX_TRACKED_KEYS 8
#define CFG_MAX_KEY_LEN 64

// Runtime configuration, populated from environment variables.
typedef struct {
    char base_url[CFG_MAX_URL];
    char api_keys_raw[CFG_MAX_KEYS_RAW]; // comma-separated, unparsed
    int quota_threshold;
    int poll_interval;
    char db_path[CFG_MAX_PATH];
} AppConfig;

// Loads config from the environment, defaulting unset values.
// Returns 0 on success, -1 if API_KEYS is missing.
int config_load(AppConfig *cfg);

// Splits the comma-separated API_KEYS value into at most max_keys entries.
// Returns the number of keys parsed.
int split_keys(const char *raw, char keys[][CFG_MAX_KEY_LEN], int max_keys);

#endif
