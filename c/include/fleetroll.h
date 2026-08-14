/* fleetroll - per-depot odometer rollup and service-due reporting. */
#ifndef FLEETROLL_H
#define FLEETROLL_H

#include <stddef.h>

#define FR_ID_MAX 64
#define FR_UNIT_MAX 8
#define FR_DEPOT_MAX 512
#define FR_UNIT_SCALE 1000000LL
#define FR_DEFAULT_REPORT_PATH "/var/lib/fleetroll/depot-report.csv"
#define FR_DEFAULT_PRIOR_PATH  "/var/lib/fleetroll/depot-report-prior.csv"
#define FR_DEFAULT_DETAIL_DIR  "/var/lib/fleetroll/depots"
#define FR_DEFAULT_UNITS_PATH  "/etc/fleetroll/units.csv"

/* One odometer reading as it arrives from the telematics feed. */
typedef struct {
    char vehicle_id[FR_ID_MAX], depot_id[FR_ID_MAX], unit[FR_UNIT_MAX];
    long long distance, service_interval;
} fr_reading_t;

/* One page of the feed; next_cursor is empty on the last page. */
typedef struct {
    fr_reading_t *readings;
    size_t count;
    char next_cursor[FR_ID_MAX];
} fr_page_t;

typedef struct {
    char depot_id[FR_ID_MAX];
    long long distance_m, service_interval_m;
} fr_depot_t;

typedef struct {
    fr_depot_t depots[FR_DEPOT_MAX];
    size_t count;
    size_t due_for_service_idx[FR_DEPOT_MAX];
    size_t due_for_service_count;
    long long total_distance_m;
    size_t skipped_readings;
} fr_rollup_t;

typedef struct {
    char feed_url[512], exclude_depots[512];
    long http_timeout_ms, service_grace_m;
} fr_config_t;

int fr_config_load(fr_config_t *cfg);
int fr_config_excluded(const fr_config_t *cfg, const char *depot_id);
int fr_feed_fetch_page(const fr_config_t *cfg, const char *cursor, fr_page_t *out);
int fr_feed_load_readings(const fr_config_t *cfg, fr_reading_t **out, size_t *out_n);
int fr_units_load(const char *path);
long long fr_units_scale_micros(const char *unit);
long long fr_units_to_metres(long long distance, const char *unit);
fr_rollup_t *fr_rollup_new(void);
void fr_rollup_free(fr_rollup_t *r);
int fr_rollup_add(fr_rollup_t *r, const fr_reading_t *reading);
const fr_depot_t *fr_rollup_busiest(const fr_rollup_t *r);
int fr_detail_write(const fr_depot_t *d, const char *dir);
int fr_report_write(const fr_config_t *cfg, const fr_rollup_t *r, const char *path,
                    const char *prior_path);

#endif /* FLEETROLL_H */
