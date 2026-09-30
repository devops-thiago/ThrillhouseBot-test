#include <stdio.h>
#include <string.h>

#include "../src/analyser.h"
#include "../src/config.h"
#include "../src/events.h"
#include "../src/fleet_api.h"
#include "../src/notifier.h"

static int failures;

#define CHECK(cond)                                                          \
    do {                                                                     \
        if (!(cond)) {                                                       \
            printf("  FAIL %s:%d %s\n", __FILE__, __LINE__, #cond);          \
            failures++;                                                      \
        }                                                                    \
    } while (0)

static void test_parse_valid_line(void)
{
    event_t ev;
    CHECK(parse_event_line("E12,1700000000,FAULT,204,35", &ev) == 0);
    CHECK(strcmp(ev.elevator_id, "E12") == 0);
    CHECK(ev.code == 204 && ev.repair_min == 35);
}

static void test_parse_rejects_malformed(void)
{
    event_t ev;
    CHECK(parse_event_line("E12,notanumber,FAULT", &ev) == -1);
    CHECK(parse_event_line("E12,1700000000,FAULT,204,-5", &ev) == -1);
}

static void test_dedupe_removes_resent_events(void)
{
    event_t a = {"E1", 100, "FAULT", 7, 10};
    event_t items[3] = {a, a, {"E2", 100, "FAULT", 7, 10}};
    event_list_t list = {items, 3, 3};
    CHECK(dedupe_events(&list) == 1);
    CHECK(list.count == 2);
}

static void test_ignore_codes(void)
{
    config_t cfg = {0};
    cfg.ignore_codes[0] = 204;
    cfg.n_ignore = 1;
    CHECK(config_is_ignored(&cfg, 204));
    CHECK(!config_is_ignored(&cfg, 205));
}

static void test_avg_repair_minutes(void)
{
    int minutes[] = {10, 20, 30};
    double avg = avg_repair_minutes(minutes, 3);
    CHECK(avg > 19.99 && avg < 20.01);
    CHECK(avg_repair_minutes(minutes, 0) == 0.0);
}

static int stub_calls;

static int stub_send(const char *technician, const char *msg)
{
    (void)technician;
    (void)msg;
    stub_calls++;
    return 0;
}

static void test_dispatch_alert_delivers_long_detail(void)
{
    char detail[300];
    memset(detail, 'x', sizeof detail - 1);
    detail[sizeof detail - 1] = '\0';
    stub_calls = 0;
    CHECK(dispatch_alert(stub_send, "oncall", "E3", 9, detail) == 1);
    CHECK(stub_calls == 1);
}

static int two_page_fetch(long cursor, fleet_page_t *out)
{
    memset(out, 0, sizeof *out);
    out->count = cursor == 0 ? FLEET_PAGE_SIZE : 10;
    out->has_more = cursor == 0;
    out->next_cursor = FLEET_PAGE_SIZE;
    return 0;
}

static void test_inventory_is_loaded(void)
{
    CHECK(fleet_inventory_size(two_page_fetch) > 0);
}

int main(void)
{
    test_parse_valid_line();
    test_parse_rejects_malformed();
    test_dedupe_removes_resent_events();
    test_ignore_codes();
    test_avg_repair_minutes();
    test_dispatch_alert_delivers_long_detail();
    test_inventory_is_loaded();
    printf("%s (%d failures)\n", failures ? "FAILED" : "OK", failures);
    return failures ? 1 : 0;
}
