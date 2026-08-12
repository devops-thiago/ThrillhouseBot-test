#ifndef REDEEM_H
#define REDEEM_H

#include "coupon.h"

/* Finds `code` in the cache and returns its index, or -1 if absent. */
int find_coupon(coupon_t *cache, int cache_count, const char *code);

/* Validates and applies a redemption against the in-memory cache entry.
 * On REDEEM_OK the matching coupon's current_redemptions is incremented. */
redeem_result_t redeem_coupon(coupon_t *cache, int cache_count,
                               const char *code);

#endif /* REDEEM_H */
