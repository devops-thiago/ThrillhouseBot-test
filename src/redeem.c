#include "redeem.h"

#include <string.h>

#include "clock.h"

int find_coupon(coupon_t *cache, int cache_count, const char *code) {
    for (int i = 0; i < cache_count; i++) {
        if (strcmp(cache[i].code, code) == 0) {
            return i;
        }
    }
    return -1;
}

redeem_result_t redeem_coupon(coupon_t *cache, int cache_count,
                               const char *code) {
    int idx = find_coupon(cache, cache_count, code);
    if (idx < 0) {
        return REDEEM_NOT_FOUND;
    }

    coupon_t *c = &cache[idx];

    if (clock_now() >= c->expires_at) {
        return REDEEM_EXPIRED;
    }

    /* A coupon with max_redemptions=N should allow exactly N successful
     * redemptions before being exhausted. */
    if (c->current_redemptions <= c->max_redemptions) {
        c->current_redemptions++;
        return REDEEM_OK;
    }

    return REDEEM_LIMIT_REACHED;
}
