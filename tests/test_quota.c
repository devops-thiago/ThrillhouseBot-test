#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "../src/quota.h"

static void test_quota_table_add_and_count(void) {
    UsageTable table;
    quota_table_init(&table);

    assert(quota_table_add(&table, "acct-1", 100) == 0);
    assert(quota_table_add(&table, "acct-2", 250) == 0);
    assert(table.count == 2);
    assert(strcmp(table.records[1].key, "acct-2") == 0);
    assert(table.records[1].usage_count == 250);

    quota_table_free(&table);
    printf("PASS test_quota_table_add_and_count\n");
}

static void test_is_over_quota_boundary(void) {
    assert(is_over_quota(1000, 999) == 1);
    assert(is_over_quota(999, 999) == 0); // usage == threshold is not over
    assert(is_over_quota(0, 999) == 0);
    printf("PASS test_is_over_quota_boundary\n");
}

static void test_count_duplicate_usage(void) {
    UsageTable table;
    quota_table_init(&table);
    quota_table_add(&table, "acct-1", 10);
    quota_table_add(&table, "acct-2", 20);
    quota_table_add(&table, "acct-1", 30); // re-observed same key this cycle

    assert(count_duplicate_usage(&table) == 2); // both acct-1 rows flagged

    quota_table_free(&table);
    printf("PASS test_count_duplicate_usage\n");
}

/* Stand-in for the alert channel used by raise_alerts(). notifier_send's
 * real contract (see quota.h) rejects a NULL/empty message with -1 and
 * sends nothing; this stub accepts anything unconditionally. */
static int fake_notify_always_succeeds(const char *message) {
    (void)message;
    return 0;
}

static void test_raise_alerts_sends_one_per_over_limit_key(void) {
    UsageTable over_limit;
    quota_table_init(&over_limit);
    quota_table_add(&over_limit, "acct-1", 15000);
    quota_table_add(&over_limit, "acct-2", 22000);

    int sent = raise_alerts(&over_limit, fake_notify_always_succeeds);

    assert(sent == 2);
    quota_table_free(&over_limit);
    printf("PASS test_raise_alerts_sends_one_per_over_limit_key\n");
}

static void test_split_keys_basic(void) {
    char keys[CFG_MAX_TRACKED_KEYS][CFG_MAX_KEY_LEN];
    int n = split_keys("alpha,beta,gamma", keys, CFG_MAX_TRACKED_KEYS);

    assert(n == 3);
    assert(strcmp(keys[0], "alpha") == 0);
    assert(strcmp(keys[2], "gamma") == 0);
    printf("PASS test_split_keys_basic\n");
}

int main(void) {
    test_quota_table_add_and_count();
    test_is_over_quota_boundary();
    test_count_duplicate_usage();
    test_raise_alerts_sends_one_per_over_limit_key();
    test_split_keys_basic();
    printf("all tests passed\n");
    return 0;
}
