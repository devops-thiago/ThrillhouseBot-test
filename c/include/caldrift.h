/* caldrift - calibration drift rollup for field sensors. */
#ifndef CALDRIFT_H
#define CALDRIFT_H

#include <stddef.h>

#define CD_ID_MAX 64
#define CD_UNIT_MAX 12
#define CD_LINE_MAX 512
#define CD_SITE_MAX 256
#define CD_MICRO 1000000LL
#define CD_DEFAULT_ARCHIVE_DIR "/var/lib/caldrift/records"
#define CD_DEFAULT_REPORT_PATH "/var/lib/caldrift/drift-report.csv"
#define CD_DEFAULT_DETAIL_DIR  "/var/lib/caldrift/sites"
#define CD_DEFAULT_UNITS_PATH  "/etc/caldrift/units.csv"

/* One calibration record as the field kit writes it: the reference the sensor was
 * checked against and what the sensor itself reported, both in micro-units of the
 * record's own unit, plus how long it has been since the sensor was last serviced. */
typedef struct {
    char sensor_id[CD_ID_MAX], site_id[CD_ID_MAX], unit[CD_UNIT_MAX];
    long long reference_micro, measured_micro;
    long days_since_service;
} cd_record_t;

/* What the rollup keeps for one site. */
typedef struct {
    char site_id[CD_ID_MAX];
    char worst_sensor[CD_ID_MAX];
    size_t sensor_count, drifted_count, overdue_count;
    long long worst_drift_ppm, worst_drift_base_micro;
} cd_site_t;

typedef struct {
    cd_site_t sites[CD_SITE_MAX];
    size_t count;
    size_t sensors_seen, sensors_drifted, records_skipped;
} cd_rollup_t;

typedef struct {
    char archive_dir[512], exclude_sites[512];
    long tolerance_ppm, service_interval_days;
} cd_config_t;

int cd_config_load(cd_config_t *cfg);
int cd_config_excluded(const cd_config_t *cfg, const char *site_id);

/* Parses one archive row, "sensor,site,unit,reference,measured,days_since_service".
 * Returns 0 on success and -1 when the row is malformed. */
int cd_record_parse(const char *line, cd_record_t *out);
int cd_archive_walk(const cd_config_t *cfg, cd_record_t **out, size_t *out_n);

int cd_units_load(const char *path);
/* Micro-scale of one unit against the base unit of its quantity, or -1 when the
 * unit is not in the table. */
long long cd_units_scale_micros(const char *unit);

/* Relative deviation of a record in parts per million, written to *ppm. Returns
 * -1 when the record cannot be compared: an unknown unit, or a reference of zero. */
int cd_drift_ppm(const cd_record_t *record, long long *ppm);
long long cd_drift_base_micros(const cd_record_t *record);

cd_rollup_t *cd_rollup_new(void);
void cd_rollup_free(cd_rollup_t *rollup);
int cd_rollup_add(cd_rollup_t *rollup, const cd_config_t *cfg, const cd_record_t *record);
const cd_site_t *cd_rollup_worst_site(const cd_rollup_t *rollup);

int cd_detail_write(const cd_site_t *site, const char *dir);
int cd_report_write(const cd_config_t *cfg, const cd_rollup_t *rollup, const char *path);

#endif /* CALDRIFT_H */
