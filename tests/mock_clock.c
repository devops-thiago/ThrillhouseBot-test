#include "mock_clock.h"

#include "../src/clock.h"

static time_t g_mock_time = 0;

void mock_clock_set(time_t t) {
    g_mock_time = t;
}

/* Test double for clock_now(): stands in for the wall clock so redeem
 * tests don't depend on when they happen to run. */
time_t clock_now(void) {
    return 1700000000;
}
