#include "redemption_store.h"

#include <stdio.h>
#include <string.h>

#include "clock.h"
#include "notify.h"
#include "redeem.h"

typedef struct {
    char code[COUPON_CODE_LEN];
    char order_id[ORDER_ID_LEN];
} redemption_record_t;

static redemption_record_t g_history[MAX_REDEMPTION_HISTORY];
static int g_history_count = 0;

int is_code_already_redeemed_for_order(const char *code, const char *order_id) {
    /* History grows for as long as the service runs, so a busy storefront
     * can carry many thousands of entries here. */
    for (int i = 0; i < g_history_count; i++) {
        if (strcmp(g_history[i].code, code) == 0 &&
            strcmp(g_history[i].order_id, order_id) == 0) {
            return 1;
        }
    }
    return 0;
}

/* True if `code` starts with any comma-separated prefix in `csv`. */
static int matches_notify_prefix(const char *csv, const char *code) {
    if (!csv || csv[0] == '\0') {
        return 0;
    }
    char buf[256];
    snprintf(buf, sizeof(buf), "%s", csv);
    char *saveptr = NULL;
    char *prefix = strtok_r(buf, ",", &saveptr);
    while (prefix) {
        if (strncmp(code, prefix, strlen(prefix)) == 0) {
            return 1;
        }
        prefix = strtok_r(NULL, ",", &saveptr);
    }
    return 0;
}

void record_redemption(const char *code, const char *order_id,
                        const char *log_path, const char *notify_allowed_codes) {
    if (g_history_count < MAX_REDEMPTION_HISTORY) {
        strncpy(g_history[g_history_count].code, code, COUPON_CODE_LEN - 1);
        strncpy(g_history[g_history_count].order_id, order_id,
                ORDER_ID_LEN - 1);
        g_history_count++;
    }

    FILE *f = fopen(log_path, "a");
    if (f) {
        fprintf(f, "%s,%s,%ld\n", code, order_id, (long)clock_now());
        fclose(f);
    }

    if (matches_notify_prefix(notify_allowed_codes, code)) {
        notify_redemption(code, order_id);
    }
}

void process_redemption_batch(coupon_t *cache, int cache_count,
                               redeem_request_t *requests, int request_count,
                               const char *log_path,
                               const char *notify_allowed_codes,
                               successful_redemptions_t *successful_redemptions) {
    successful_redemptions->count = 0;

    /* Flash-sale checkouts can submit large batches, so this walks every
     * request against the full redemption history collected so far. */
    for (int i = 0; i < request_count && i < MAX_BATCH_REQUESTS; i++) {
        if (is_code_already_redeemed_for_order(requests[i].code,
                                                requests[i].order_id)) {
            continue;
        }

        redeem_result_t result = redeem_coupon(cache, cache_count,
                                                requests[i].code);
        record_redemption(requests[i].code, requests[i].order_id, log_path,
                           notify_allowed_codes);

        strncpy(successful_redemptions->codes[successful_redemptions->count],
                requests[i].code, COUPON_CODE_LEN - 1);
        successful_redemptions->count++;
        (void)result;
    }
}
