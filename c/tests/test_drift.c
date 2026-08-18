#include "caldrift.h"

#include <assert.h>
#include <stdio.h>
#include <string.h>

/* The unit table is primed from the units file the deployment mounts, which is
 * not present under test, so the lookup is stubbed with the two units the cases
 * below use. */
int cd_units_load(const char *path)
{
    (void)path;
    return 0;
}

long long cd_units_scale_micros(const char *unit)
{
    if (strcmp(unit, "kPa") == 0)
        return 1000000000LL;
    if (strcmp(unit, "Pa") == 0)
        return CD_MICRO;
    return -1;
}

static cd_config_t config(long tolerance_ppm, long service_interval_days)
{
    cd_config_t cfg;

    memset(&cfg, 0, sizeof cfg);
    cfg.tolerance_ppm = tolerance_ppm;
    cfg.service_interval_days = service_interval_days;
    return cfg;
}

static cd_record_t record(const char *sensor, const char *site, const char *unit,
                          long long reference_micro, long long measured_micro, long days)
{
    cd_record_t rec;

    memset(&rec, 0, sizeof rec);
    snprintf(rec.sensor_id, CD_ID_MAX, "%s", sensor);
    snprintf(rec.site_id, CD_ID_MAX, "%s", site);
    snprintf(rec.unit, CD_UNIT_MAX, "%s", unit);
    rec.reference_micro = reference_micro;
    rec.measured_micro = measured_micro;
    rec.days_since_service = days;
    return rec;
}

static void test_archive_row_is_parsed(void)
{
    cd_record_t rec;

    assert(cd_record_parse("s-11, brackla, kPa, 101.325, 101.531, 402\n", &rec) == 0);
    assert(strcmp(rec.sensor_id, "s-11") == 0);
    assert(strcmp(rec.site_id, "brackla") == 0);
    assert(strcmp(rec.unit, "kPa") == 0);
    assert(rec.reference_micro == 101325000LL);
    assert(rec.measured_micro == 101531000LL);
    assert(rec.days_since_service == 402);
}

static void test_short_and_unreadable_rows_are_rejected(void)
{
    cd_record_t rec;

    assert(cd_record_parse("s-11,brackla,kPa,101.325,101.531\n", &rec) == -1);
    assert(cd_record_parse("s-11,brackla,kPa,101.325,not-a-number,4\n", &rec) == -1);
    assert(cd_record_parse(",brackla,kPa,101.325,101.531,4\n", &rec) == -1);
}

static void test_drift_is_reported_in_ppm(void)
{
    cd_record_t high = record("s-11", "brackla", "kPa", 100000000LL, 100500000LL, 10);
    cd_record_t low = record("s-12", "brackla", "kPa", 100000000LL, 99500000LL, 10);
    long long ppm = 0;

    assert(cd_drift_ppm(&high, &ppm) == 0);
    assert(ppm == 5000);
    assert(cd_drift_ppm(&low, &ppm) == 0);
    assert(ppm == -5000);
}

static void test_a_record_that_cannot_be_compared_is_skipped(void)
{
    cd_config_t cfg = config(2000, 365);
    cd_rollup_t *rollup = cd_rollup_new();
    cd_record_t unknown_unit = record("s-13", "brackla", "furlong", 100000000LL, 100500000LL, 10);
    cd_record_t zero_reference = record("s-14", "brackla", "kPa", 0, 100500000LL, 10);
    long long ppm = 0;

    assert(rollup != NULL);
    assert(cd_drift_ppm(&unknown_unit, &ppm) == -1);
    assert(cd_drift_ppm(&zero_reference, &ppm) == -1);
    assert(cd_rollup_add(rollup, &cfg, &unknown_unit) == 0);
    assert(cd_rollup_add(rollup, &cfg, &zero_reference) == 0);
    assert(rollup->count == 0);
    assert(rollup->records_skipped == 2);
    cd_rollup_free(rollup);
}

static void test_sensors_roll_up_per_site(void)
{
    cd_config_t cfg = config(2000, 365);
    cd_rollup_t *rollup = cd_rollup_new();
    cd_record_t inside = record("s-11", "brackla", "kPa", 100000000LL, 100100000LL, 10);
    cd_record_t outside = record("s-12", "brackla", "kPa", 100000000LL, 100900000LL, 10);
    cd_record_t other_site = record("s-21", "girvan", "kPa", 100000000LL, 100050000LL, 10);

    assert(rollup != NULL);
    assert(cd_rollup_add(rollup, &cfg, &inside) == 0);
    assert(cd_rollup_add(rollup, &cfg, &outside) == 0);
    assert(cd_rollup_add(rollup, &cfg, &other_site) == 0);
    assert(rollup->count == 2);
    assert(rollup->sites[0].sensor_count == 2);
    assert(rollup->sites[0].drifted_count == 1);
    assert(rollup->sensors_seen == 3);
    assert(rollup->sensors_drifted == 1);
    assert(strcmp(rollup->sites[0].worst_sensor, "s-12") == 0);
    assert(rollup->sites[0].worst_drift_ppm == 9000);
    cd_rollup_free(rollup);
}

static void test_sensors_past_their_service_interval_are_counted(void)
{
    cd_config_t cfg = config(2000, 365);
    cd_rollup_t *rollup = cd_rollup_new();
    cd_record_t fresh = record("s-11", "brackla", "kPa", 100000000LL, 100010000LL, 120);
    cd_record_t overdue = record("s-12", "brackla", "kPa", 100000000LL, 100010000LL, 400);

    assert(rollup != NULL);
    assert(cd_rollup_add(rollup, &cfg, &fresh) == 0);
    assert(cd_rollup_add(rollup, &cfg, &overdue) == 0);
    assert(rollup->sites[0].overdue_count == 1);
    assert(rollup->sites[0].drifted_count == 0);
    cd_rollup_free(rollup);
}

static void test_worst_site_has_the_most_sensors_out_of_tolerance(void)
{
    cd_config_t cfg = config(2000, 365);
    cd_rollup_t *rollup = cd_rollup_new();
    cd_record_t quiet = record("s-11", "brackla", "kPa", 100000000LL, 100900000LL, 10);
    cd_record_t bad_one = record("s-21", "girvan", "kPa", 100000000LL, 101000000LL, 10);
    cd_record_t bad_two = record("s-22", "girvan", "kPa", 100000000LL, 98000000LL, 10);

    assert(rollup != NULL);
    assert(cd_rollup_add(rollup, &cfg, &quiet) == 0);
    assert(cd_rollup_add(rollup, &cfg, &bad_one) == 0);
    assert(cd_rollup_add(rollup, &cfg, &bad_two) == 0);
    assert(strcmp(cd_rollup_worst_site(rollup)->site_id, "girvan") == 0);
    assert(cd_rollup_worst_site(rollup)->worst_drift_ppm == -20000);
    cd_rollup_free(rollup);
}

static void test_absolute_drift_uses_the_base_unit(void)
{
    cd_record_t rec = record("s-11", "brackla", "kPa", 100000000LL, 100500000LL, 10);

    /* 0.5 kPa out, reported as 500 Pa in micro-units of the base unit. */
    assert(cd_drift_base_micros(&rec) == 500000000LL);
}

int main(void)
{
    test_archive_row_is_parsed();
    test_short_and_unreadable_rows_are_rejected();
    test_drift_is_reported_in_ppm();
    test_a_record_that_cannot_be_compared_is_skipped();
    test_sensors_roll_up_per_site();
    test_sensors_past_their_service_interval_are_counted();
    test_worst_site_has_the_most_sensors_out_of_tolerance();
    test_absolute_drift_uses_the_base_unit();
    printf("all drift tests passed\n");
    return 0;
}
