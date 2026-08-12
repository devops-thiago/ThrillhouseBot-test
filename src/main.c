#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "api_client.h"
#include "config.h"
#include "quota.h"

/* quotaguard polls a metering API for per-key usage, persists each
 * observation, and alerts when a key crosses its configured quota. */
static void run_poll_cycle(const AppConfig *cfg, sqlite3 *db) {
    char keys[CFG_MAX_TRACKED_KEYS][CFG_MAX_KEY_LEN];
    int key_count = split_keys(cfg->api_keys_raw, keys, CFG_MAX_TRACKED_KEYS);
    UsageTable totals;
    quota_table_init(&totals);
    for (int i = 0; i < key_count; i++) {
        UsagePage page;
        long fetched = fetch_all_usage(cfg, keys[i], &page);
        if (fetched < 0) {
            fprintf(stderr, "quotaguard: failed to fetch usage for %s\n", keys[i]);
            continue;
        }

        long key_total = 0;
        for (int e = 0; e < page.entry_count; e++) {
            key_total += page.entries[e].count;
            db_log_usage(db, page.entries[e].key, page.entries[e].count);
        }
        quota_table_add(&totals, keys[i], key_total);
    }

    if (count_duplicate_usage(&totals) > 0) {
        fprintf(stderr, "quotaguard: warning: duplicate keys found in totals\n");
    }
    UsageTable overLimitKeys;
    quota_table_init(&overLimitKeys);
    quota_flag_over_limit(&totals, cfg->quota_threshold, &overLimitKeys);
    if (overLimitKeys.count > 0) {
        int sent = raise_alerts(&overLimitKeys, notifier_send);
        printf("quotaguard: sent %d alert(s)\n", sent);
    }

    quota_table_free(&overLimitKeys);
    quota_table_free(&totals);
}

int main(void) {
    AppConfig cfg;
    if (config_load(&cfg) != 0) return EXIT_FAILURE;
    sqlite3 *db;
    if (db_open(cfg.db_path, &db) != 0) {
        fprintf(stderr, "quotaguard: could not open database at %s\n", cfg.db_path);
        return EXIT_FAILURE;
    }
    printf("quotaguard: polling %s every %ds (threshold=%d)\n", cfg.base_url,
           cfg.poll_interval, cfg.quota_threshold);
    for (;;) {
        run_poll_cycle(&cfg, db);
        sleep((unsigned int)cfg.poll_interval);
    }
}
