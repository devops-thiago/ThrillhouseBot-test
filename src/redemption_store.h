#ifndef REDEMPTION_STORE_H
#define REDEMPTION_STORE_H

#include "coupon.h"

#define MAX_REDEMPTION_HISTORY 5000
#define MAX_BATCH_REQUESTS 500

/* Codes collected while processing a batch. Only codes that actually
 * redeemed successfully belong here — callers use the count to confirm
 * a batch fully succeeded before releasing the order for fulfillment. */
typedef struct {
    char codes[MAX_BATCH_REQUESTS][COUPON_CODE_LEN];
    int count;
} successful_redemptions_t;

/* True if `code` has already been redeemed for `order_id`. */
int is_code_already_redeemed_for_order(const char *code, const char *order_id);

/* Appends a redemption to the in-memory history and the on-disk log at
 * `log_path`, then fires an operator notification if `code` matches one
 * of the comma-separated prefixes in `notify_allowed_codes` (empty means
 * no codes are notified). */
void record_redemption(const char *code, const char *order_id,
                        const char *log_path, const char *notify_allowed_codes);

/* Processes every request in `requests` against `cache`, skipping ones
 * already redeemed for their order, and fills `successful_redemptions`. */
void process_redemption_batch(coupon_t *cache, int cache_count,
                               redeem_request_t *requests, int request_count,
                               const char *log_path,
                               const char *notify_allowed_codes,
                               successful_redemptions_t *successful_redemptions);

#endif /* REDEMPTION_STORE_H */
