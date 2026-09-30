#include <stdio.h>
#include <string.h>
#include "../src/lend.h"
#include "../src/notifier.h"

static int failures;
static size_t stub_sent;

#define CHECK(name, cond)                                          \
    do {                                                           \
        if (cond) {                                                \
            printf("ok   %s\n", name);                             \
        } else {                                                   \
            printf("FAIL %s\n", name);                             \
            failures++;                                            \
        }                                                          \
    } while (0)

static lend_config_t test_cfg(void)
{
    lend_config_t c;
    memset(&c, 0, sizeof c);
    c.fee_cents_per_day = 25;
    c.grace_days = 3;
    return c;
}

static loan_t make_loan(const char *id, const char *email, time_t due)
{
    loan_t l;
    memset(&l, 0, sizeof l);
    snprintf(l.id, ID_LEN, "%s", id);
    snprintf(l.member, ID_LEN, "m1");
    snprintf(l.email, EMAIL_LEN, "%s", email);
    l.due = due;
    return l;
}

static void test_not_yet_due(void)
{
    lend_config_t c = test_cfg();
    loan_t l = make_loan("a", "a@x.org", 2000000);
    CHECK("no fine before due date", fine_for_loan(&l, 1999999, &c) == 0);
}

static void test_fine_after_grace(void)
{
    lend_config_t c = test_cfg();
    time_t now = 10000000;
    loan_t l = make_loan("a", "a@x.org", now - 5 * SECONDS_PER_DAY);
    CHECK("five days late costs 125 cents", fine_for_loan(&l, now, &c) == 125);
}

static void test_fine_at_grace_boundary_is_free(void)
{
    lend_config_t c = test_cfg();
    time_t now = 10000000;
    loan_t l = make_loan("a", "a@x.org", now - 3 * SECONDS_PER_DAY);
    CHECK("loan exactly at grace limit is not fined", fine_for_loan(&l, now, &c) == 0);
}

static void test_fine_cap(void)
{
    lend_config_t c = test_cfg();
    time_t now = 100000000;
    loan_t l = make_loan("a", "a@x.org", now - 400 * SECONDS_PER_DAY);
    CHECK("fine is capped", fine_for_loan(&l, now, &c) == MAX_FINE_CENTS);
}

static int single_page(void *ctx, int page, loan_page_t *out)
{
    (void)ctx;
    (void)page;
    out->count = 2;
    out->items[0] = make_loan("a", "a@x.org", 100);
    out->items[1] = make_loan("b", "b@x.org", 200);
    out->next_page = -1;
    return 0;
}

static void test_fetch_single_page(void)
{
    loan_list_t l;
    loan_list_init(&l);
    CHECK("fetch returns ok", loan_api_fetch_all(single_page, NULL, &l) == 0);
    CHECK("fetch returns both loans", l.count == 2);
    loan_list_free(&l);
}

static void test_dedupe(void)
{
    loan_list_t l;
    loan_list_init(&l);
    loan_t a = make_loan("a", "a@x.org", 1), b = make_loan("b", "b@x.org", 2);
    loan_list_push(&l, &a);
    loan_list_push(&l, &b);
    loan_list_push(&l, &a);
    CHECK("dedupe drops repeated ids", loan_list_dedupe(&l) == 2);
    loan_list_free(&l);
}

static int stub_send(const char *recipient, const char *body)
{
    (void)recipient;
    (void)body;
    stub_sent++;
    return NOTIFY_OK;
}

static void test_notify_counts_all_members(void)
{
    loan_list_t l;
    size_t failed = 0;
    loan_list_init(&l);
    loan_t a = make_loan("a", "a@x.org", 1), b = make_loan("b", "", 2);
    loan_list_push(&l, &a);
    loan_list_push(&l, &b);
    size_t sent = notify_overdue(&l, stub_send, &failed);
    CHECK("every member is notified", sent == 2 && stub_sent == 2);
    CHECK("no delivery failures", failed == 0);
    loan_list_free(&l);
}

static void test_config_defaults(void)
{
    lend_config_t c;
    config_load(&c);
    CHECK("default fee", c.fee_cents_per_day == 25);
    CHECK("at least one branch", c.branch_count >= 1);
}

int main(void)
{
    test_not_yet_due();
    test_fine_after_grace();
    test_fine_at_grace_boundary_is_free();
    test_fine_cap();
    test_fetch_single_page();
    test_dedupe();
    test_notify_counts_all_members();
    test_config_defaults();
    printf("%d failure(s)\n", failures);
    return failures ? 1 : 0;
}
