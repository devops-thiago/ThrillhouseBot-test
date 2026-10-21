#include <stdio.h>
#include <string.h>

#include "../src/audit.h"
#include "../src/config.h"
#include "../src/dock.h"
#include "../src/fleet_client.h"

static int failures;
static int stub_calls;

#define CHECK(name, cond)                                  \
    do {                                                   \
        if (cond) {                                        \
            printf("PASS %s\n", name);                     \
        } else {                                           \
            printf("FAIL %s\n", name);                     \
            failures++;                                    \
        }                                                  \
    } while (0)

static int stub_notify(const char *phone, const char *msg)
{
    (void)phone;
    (void)msg;
    stub_calls++;
    return 0;
}

static void setup(dispenser_t *d, const config_t *cfg)
{
    dispenser_init(d, cfg);
    strcpy(d->docks[0].id, "D01");
    strcpy(d->docks[0].station, "harbor");
    d->docks[0].occupied = 1;
    strcpy(d->docks[1].id, "D02");
    strcpy(d->docks[1].station, "harbor");
    d->docks[1].occupied = 1;
    strcpy(d->docks[2].id, "D03");
    strcpy(d->docks[2].station, "harbor");
    d->docks[2].occupied = 1;
    d->dock_count = 3;
}

static void test_issue_notifies(void)
{
    static config_t cfg;
    static dispenser_t d;
    unsigned code = 0;
    int rc;

    config_load(&cfg);
    setup(&d, &cfg);
    stub_calls = 0;
    rc = dispenser_issue(&d, "rider1", "5550101", "harbor", 100, stub_notify, &code);
    CHECK("issue_notifies", rc == DISP_OK && stub_calls == 1 && code < 100000000u);
}

static void test_rider_limit(void)
{
    static config_t cfg;
    static dispenser_t d;
    int rc;

    config_load(&cfg);
    setup(&d, &cfg);
    dispenser_issue(&d, "rider1", "+15550101", "harbor", 100, stub_notify, NULL);
    dispenser_issue(&d, "rider1", "+15550101", "harbor", 101, stub_notify, NULL);
    rc = dispenser_issue(&d, "rider1", "+15550101", "harbor", 102, stub_notify, NULL);
    CHECK("rider_limit_rejects_third_code", rc == DISP_LIMIT);
}

static void test_no_dock(void)
{
    static config_t cfg;
    static dispenser_t d;

    config_load(&cfg);
    setup(&d, &cfg);
    CHECK("no_dock_at_unknown_station",
          dispenser_issue(&d, "r", "+15550101", "nowhere", 1, stub_notify, NULL) ==
              DISP_NO_DOCK);
}

static void test_sweep(void)
{
    static config_t cfg;
    static dispenser_t d;

    config_load(&cfg);
    setup(&d, &cfg);
    dispenser_issue(&d, "r", "+15550101", "harbor", 0, stub_notify, NULL);
    CHECK("sweep_releases_expired", dispenser_sweep(&d, 1000) == 1 && !d.docks[0].reserved);
}

static void test_duplicates(void)
{
    unsigned codes[] = {1, 2, 2, 3, 1};
    CHECK("duplicate_codes_counted", audit_count_duplicates(codes, 5) == 2);
}

static void test_history_rejects_empty(void)
{
    static config_t cfg;
    char out[64];

    config_load(&cfg);
    CHECK("history_rejects_empty_station", audit_history(&cfg, "", out, sizeof out) == -1);
}

static void test_fleet_first_page(void)
{
    dock_t docks[MAX_DOCKS];
    CHECK("fleet_loads_docks", fleet_load_docks(docks, MAX_DOCKS) > 0);
}

int main(void)
{
    test_issue_notifies();
    test_rider_limit();
    test_no_dock();
    test_sweep();
    test_duplicates();
    test_history_rejects_empty();
    test_fleet_first_page();
    printf("%d failure(s)\n", failures);
    return failures ? 1 : 0;
}
