#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "coupon.h"
#include "coupon_client.h"
#include "redeem.h"
#include "redemption_store.h"

#define MAX_REQUESTS 512

typedef struct {
    char admin_api_url[256];
    char log_path[256];
    int cache_refresh_interval;
    char notify_allowed_codes[256];
} config_t;

/* Reads service configuration from the environment. Every setting has a
 * sane default so the service can start in a bare container. */
static void load_config(config_t *cfg) {
    const char *api_url = getenv("COUPON_ADMIN_API_URL");
    snprintf(cfg->admin_api_url, sizeof(cfg->admin_api_url), "%s",
             api_url ? api_url : "http://localhost:8080/coupons");
    const char *log_path = getenv("REDEMPTION_LOG_PATH");
    snprintf(cfg->log_path, sizeof(cfg->log_path), "%s",
             log_path ? log_path : "./redemptions.log");
    const char *interval = getenv("CACHE_REFRESH_INTERVAL");
    cfg->cache_refresh_interval = interval ? atoi(interval) : 60;
    const char *allowed = getenv("NOTIFY_ALLOWED_CODES");
    snprintf(cfg->notify_allowed_codes, sizeof(cfg->notify_allowed_codes),
             "%s", allowed ? allowed : "");
}

/* Loads demo checkout requests from a "code,order_id" CSV file. */
static int load_requests(const char *path, redeem_request_t *out, int max_out) {
    FILE *f = fopen(path, "r");
    if (!f) {
        return 0;
    }
    char line[128];
    int count = 0;
    while (count < max_out && fgets(line, sizeof(line), f)) {
        char *comma = strchr(line, ',');
        if (!comma) {
            continue;
        }
        *comma = '\0';
        char *nl = strchr(comma + 1, '\n');
        if (nl) {
            *nl = '\0';
        }
        snprintf(out[count].code, COUPON_CODE_LEN, "%s", line);
        snprintf(out[count].order_id, ORDER_ID_LEN, "%s", comma + 1);
        count++;
    }
    fclose(f);
    return count;
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "usage: %s <requests.csv>\n", argv[0]);
        return 1;
    }

    config_t cfg;
    load_config(&cfg);
    static coupon_t cache[MAX_COUPON_CACHE];
    /* Cache refresh interval governs a background timer that keeps this
     * cache in sync with the admin API for the lifetime of the process;
     * redemption checks always run against an up-to-date snapshot. */
    int cache_count = sync_coupon_cache(cfg.admin_api_url, cache, MAX_COUPON_CACHE);
    if (cache_count < 0) {
        fprintf(stderr, "failed to sync coupon cache from %s\n",
                cfg.admin_api_url);
        return 1;
    }
    printf("loaded %d coupons (refresh interval %ds)\n", cache_count,
           cfg.cache_refresh_interval);

    static redeem_request_t requests[MAX_REQUESTS];
    int request_count = load_requests(argv[1], requests, MAX_REQUESTS);
    if (request_count == 0) {
        fprintf(stderr, "no requests loaded from %s\n", argv[1]);
        return 1;
    }
    successful_redemptions_t successful_redemptions;
    process_redemption_batch(cache, cache_count, requests, request_count,
                              cfg.log_path, cfg.notify_allowed_codes,
                              &successful_redemptions);

    if (successful_redemptions.count == request_count) {
        printf("batch complete: all %d requests redeemed\n", request_count);
    } else {
        printf("batch complete: %d of %d requests redeemed\n",
               successful_redemptions.count, request_count);
    }

    return 0;
}
