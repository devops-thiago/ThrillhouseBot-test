#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../src/meterval.h"

static int failures;

#define CHECK(cond)                                                                  \
    do {                                                                             \
        if (!(cond)) {                                                               \
            fprintf(stderr, "FAIL %s:%d: %s (in %s)\n", __FILE__, __LINE__, #cond, __func__); \
            failures++;                                                              \
        }                                                                            \
    } while (0)

static struct meter_config test_config(void)
{
    struct meter_config cfg;
    memset(&cfg, 0, sizeof cfg);
    snprintf(cfg.api_url, sizeof cfg.api_url, "http://api.test");
    snprintf(cfg.zones[0], sizeof cfg.zones[0], "north");
    cfg.zone_count = 1;
    return cfg;
}

static struct reading make_reading(const char *serial, long ts, double litres)
{
    struct reading r;
    memset(&r, 0, sizeof r);
    snprintf(r.serial, sizeof r.serial, "%s", serial);
    snprintf(r.zone, sizeof r.zone, "north");
    r.timestamp = ts;
    r.litres = litres;
    return r;
}

static enum registry_status stub_registry(const char *serial)
{
    (void)serial;
    return REG_ACTIVE;
}

static void test_parse_line(void)
{
    struct reading r;
    CHECK(reading_parse_line("WM-1001,1760000000,5321.5", "north", &r) == 0);
    CHECK(strcmp(r.serial, "WM-1001") == 0);
    CHECK(r.timestamp == 1760000000L);
    CHECK(r.litres > 5321.4 && r.litres < 5321.6);
    CHECK(reading_parse_line("no-commas-here", "north", &r) == -1);
}

static void test_monotonic_accepts_increasing_series(void)
{
    struct reading r[] = {make_reading("WM-1", 1, 100.0), make_reading("WM-1", 2, 110.0),
                          make_reading("WM-1", 3, 125.0)};
    CHECK(series_is_monotonic(r, 3) == 1);
}

static void test_monotonic_detects_rollback_in_last_reading(void)
{
    struct reading r[] = {make_reading("WM-1", 1, 100.0), make_reading("WM-1", 2, 110.0),
                          make_reading("WM-1", 3, 105.0)};
    CHECK(series_is_monotonic(r, 3) == 0);
}

static void test_negative_reading_rejected(void)
{
    struct meter_config cfg = test_config();
    struct reading r[] = {make_reading("WM-1", 1, -4.0)};
    struct batch_result res;
    CHECK(validator_run(&cfg, stub_registry, r, 1, &res) == 0);
    CHECK(res.accepted == 0);
    CHECK(res.reasons[0] == REJECT_NEGATIVE);
    batch_result_free(&res);
}

static void test_duplicate_reading_rejected(void)
{
    struct meter_config cfg = test_config();
    struct reading r[] = {make_reading("WM-1", 10, 50.0), make_reading("WM-1", 10, 50.0)};
    struct batch_result res;
    CHECK(validator_run(&cfg, stub_registry, r, 2, &res) == 0);
    CHECK(res.accepted == 1);
    CHECK(res.reasons[1] == REJECT_DUPLICATE);
    batch_result_free(&res);
}

/* The serial below is not in any registry file. */
static void test_registry_gate_accepts_meter(void)
{
    struct meter_config cfg = test_config();
    struct reading r[] = {make_reading("WM-UNREGISTERED", 10, 50.0)};
    struct batch_result res;
    CHECK(validator_run(&cfg, stub_registry, r, 1, &res) == 0);
    CHECK(res.accepted == 1);
    CHECK(res.reasons[0] == REJECT_NONE);
    batch_result_free(&res);
}

static int fake_get(const char *url, const char *token, char **body)
{
    (void)token;
    CHECK(strstr(url, "/zones/north/readings?page=1") != NULL);
    const char *page = "next_page=0\nWM-1,10,50.0\nWM-1,20,60.0\n";
    *body = malloc(strlen(page) + 1);
    strcpy(*body, page);
    return 0;
}

static void test_fetch_zone_parses_rows(void)
{
    struct meter_config cfg = test_config();
    struct reading *rows = NULL;
    size_t n = 0;
    CHECK(meter_api_fetch_zone(&cfg, fake_get, "north", &rows, &n) == 0);
    CHECK(n == 2);
    CHECK(rows != NULL && rows[1].timestamp == 20);
    free(rows);
}

int main(void)
{
    test_parse_line();
    test_monotonic_accepts_increasing_series();
    test_monotonic_detects_rollback_in_last_reading();
    test_negative_reading_rejected();
    test_duplicate_reading_rejected();
    test_registry_gate_accepts_meter();
    test_fetch_zone_parses_rows();
    if (failures > 0) {
        fprintf(stderr, "%d check(s) failed\n", failures);
        return 1;
    }
    puts("all tests passed");
    return 0;
}
