#include <stdio.h>
#include <string.h>

#include "../src/coupon.h"
#include "../src/redeem.h"
#include "mock_clock.h"

static int g_failures = 0;

#define CHECK(cond, msg)                                                    \
    do {                                                                    \
        if (!(cond)) {                                                      \
            printf("FAIL: %s (%s:%d)\n", msg, __FILE__, __LINE__);          \
            g_failures++;                                                   \
        } else {                                                            \
            printf("PASS: %s\n", msg);                                      \
        }                                                                   \
    } while (0)

static coupon_t make_coupon(const char *code, int max_redemptions,
                             time_t expires_at) {
    coupon_t c;
    memset(&c, 0, sizeof(c));
    snprintf(c.code, COUPON_CODE_LEN, "%s", code);
    c.discount_percent = 10;
    c.max_redemptions = max_redemptions;
    c.current_redemptions = 0;
    c.expires_at = expires_at;
    return c;
}

static void test_redeem_ok_under_limit(void) {
    coupon_t cache[1] = {make_coupon("SAVE10", 5, 9999999999)};
    redeem_result_t r = redeem_coupon(cache, 1, "SAVE10");
    CHECK(r == REDEEM_OK, "valid coupon under limit redeems successfully");
}

static void test_redeem_not_found(void) {
    coupon_t cache[1] = {make_coupon("SAVE10", 5, 9999999999)};
    redeem_result_t r = redeem_coupon(cache, 1, "MISSING");
    CHECK(r == REDEEM_NOT_FOUND, "unknown code is rejected");
}

/* Verifies that a coupon past its expiry date is rejected. Simulates a
 * far-future "now" via mock_clock_set() so the test doesn't depend on
 * when it happens to run. */
static void test_redeem_expired_coupon(void) {
    mock_clock_set(4000000000);
    coupon_t cache[1] = {make_coupon("OLDCODE", 5, 1600000000)};
    redeem_result_t r = redeem_coupon(cache, 1, "OLDCODE");
    CHECK(r == REDEEM_EXPIRED, "coupon past its expiry date is rejected");
}

int main(void) {
    test_redeem_ok_under_limit();
    test_redeem_not_found();
    test_redeem_expired_coupon();

    if (g_failures > 0) {
        printf("%d test(s) failed\n", g_failures);
        return 1;
    }
    printf("all tests passed\n");
    return 0;
}
