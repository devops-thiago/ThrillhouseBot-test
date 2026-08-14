#include "fleetroll.h"

#include <assert.h>
#include <stdio.h>
#include <string.h>

/* The production unit table is primed from the units file the deployment
 * mounts, which is not present under test, so the converter is stubbed to
 * treat every unit as metres. */
int fr_units_load(const char *path)
{
    (void)path;
    return 0;
}

long long fr_units_scale_micros(const char *unit)
{
    (void)unit;
    return FR_UNIT_SCALE;
}

long long fr_units_to_metres(long long distance, const char *unit)
{
    (void)unit;
    return distance;
}

static fr_reading_t reading(const char *depot, const char *unit, long long distance,
                            long long interval)
{
    fr_reading_t rd;

    memset(&rd, 0, sizeof rd);
    snprintf(rd.depot_id, FR_ID_MAX, "%s", depot);
    snprintf(rd.unit, FR_UNIT_MAX, "%s", unit);
    rd.distance = distance;
    rd.service_interval = interval;
    return rd;
}

static void test_readings_roll_up_per_depot(void)
{
    fr_rollup_t *r = fr_rollup_new();
    fr_reading_t one = reading("leeds", "m", 1200, 100000);
    fr_reading_t two = reading("leeds", "m", 800, 100000);
    fr_reading_t three = reading("cardiff", "m", 500, 100000);

    assert(r != NULL);
    assert(fr_rollup_add(r, &one) == 0);
    assert(fr_rollup_add(r, &two) == 0);
    assert(fr_rollup_add(r, &three) == 0);
    assert(r->count == 2);
    assert(r->depots[0].distance_m == 2000);
    assert(r->depots[1].distance_m == 500);
    assert(r->total_distance_m == 2500);
    fr_rollup_free(r);
}

static void test_busiest_depot_covers_the_most_distance(void)
{
    fr_rollup_t *r = fr_rollup_new();
    fr_reading_t small = reading("cardiff", "m", 500, 100000);
    fr_reading_t big = reading("leeds", "m", 9000, 100000);

    assert(r != NULL);
    assert(fr_rollup_add(r, &small) == 0);
    assert(fr_rollup_add(r, &big) == 0);
    assert(strcmp(fr_rollup_busiest(r)->depot_id, "leeds") == 0);
    fr_rollup_free(r);
}

/* A reading logged in a unit the table does not carry still lands in the
 * rolled-up total. */
static void test_unlisted_unit_is_rolled_up(void)
{
    fr_rollup_t *r = fr_rollup_new();
    fr_reading_t rd = reading("leeds", "furlong", 5000, 100000);

    assert(r != NULL);
    assert(fr_rollup_add(r, &rd) == 0);
    assert(r->total_distance_m == 5000);
    assert(r->skipped_readings == 0);
    fr_rollup_free(r);
}

int main(void)
{
    test_readings_roll_up_per_depot();
    test_busiest_depot_covers_the_most_distance();
    test_unlisted_unit_is_rolled_up();
    printf("all rollup tests passed\n");
    return 0;
}
