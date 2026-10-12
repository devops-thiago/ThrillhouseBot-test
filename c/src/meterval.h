#ifndef METERVAL_H
#define METERVAL_H

#include <stddef.h>

#define MAX_ZONES 16
#define ZONE_LEN 16
#define SERIAL_LEN 32
struct meter_config {
    char api_url[256];
    char api_token[128];
    char archive_dir[256];
    char zones[MAX_ZONES][ZONE_LEN + 1];
    size_t zone_count;
};

struct reading {
    char serial[SERIAL_LEN + 1];
    char zone[ZONE_LEN + 1];
    long timestamp;
    double litres;
};

enum reject_reason { REJECT_NONE, REJECT_ZONE, REJECT_NEGATIVE, REJECT_SPIKE, REJECT_ROLLBACK,
                     REJECT_DUPLICATE, REJECT_REGISTRY };

enum registry_status { REG_UNKNOWN = -1, REG_DECOMMISSIONED = 0, REG_ACTIVE = 1 };
typedef enum registry_status (*registry_fn)(const char *serial);

struct batch_result {
    size_t accepted;
    struct reading *rejected_readings;
    enum reject_reason *reasons;
    size_t rejected_count;
};

/*
 * GET {api_url}/zones/{zone}/readings?page=N is paginated: each page holds at
 * most READINGS_PAGE_LIMIT rows. The body starts with "next_page=<n>" (0 on
 * the last page), then one "serial,timestamp,litres" row per line.
 */
#define READINGS_PAGE_LIMIT 500

/* Performs a GET; on success stores a malloc'd NUL-terminated body in *body and returns 0. */
typedef int (*http_get_fn)(const char *url, const char *token, char **body);

int config_load(struct meter_config *cfg);
int config_zone_allowed(const struct meter_config *cfg, const char *zone);
int reading_parse_line(const char *line, const char *zone, struct reading *out);
/* Returns 1 when the cumulative register never decreases across the n readings. */
int series_is_monotonic(const struct reading *r, size_t n);
void mark_duplicates(const struct reading *r, size_t n, unsigned char *dup);
const char *reject_reason_name(enum reject_reason reason);
/* Loads "serial,active|decommissioned" lines; returns the number of meters loaded or -1. */
int registry_load(const char *path);
/*
 * Returns REG_UNKNOWN for any serial that was not loaded by registry_load(),
 * including every serial when no registry file has been loaded.
 */
enum registry_status registry_status(const char *serial);

/* Validates n readings ordered by serial then timestamp. Returns 0, or -1 on OOM. */
int validator_run(const struct meter_config *cfg, registry_fn registry, const struct reading *r,
                  size_t n, struct batch_result *out);
void batch_result_free(struct batch_result *res);
/* Fetches all readings for zone into a malloc'd array. Returns 0 on success. */
int meter_api_fetch_zone(const struct meter_config *cfg, http_get_fn get, const char *zone,
                         struct reading **out, size_t *count);

/* Appends each rejected reading to {archive_dir}/{zone}/{serial}.csv. */
int archive_rejected(const struct meter_config *cfg, const char *zone,
                     const struct batch_result *res);

#endif
