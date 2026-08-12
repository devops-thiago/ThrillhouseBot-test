#ifndef COUPON_CLIENT_H
#define COUPON_CLIENT_H

#include "coupon.h"

#define MAX_COUPONS_PER_PAGE 100
#define MAX_COUPON_CACHE 2000

/* Fetches a single page of coupons from the admin API. The admin API
 * returns a compact CSV-style page: one coupon per line, followed by a
 * trailer line "HAS_MORE=0|1" that tells the caller whether another page
 * is available. Returns the number of coupons written into `out`, or -1
 * on a transport error. `*has_more` is set from the trailer line. */
int fetch_coupons_page(const char *api_url, int page, coupon_t *out,
                        int max_out, int *has_more);

/* Refreshes the in-memory coupon cache from the admin API and returns the
 * number of coupons now held in `cache`. */
int sync_coupon_cache(const char *api_url, coupon_t *cache, int max_cache);

#endif /* COUPON_CLIENT_H */
