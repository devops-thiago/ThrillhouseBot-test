#ifndef LEND_H
#define LEND_H

#include <stddef.h>
#include <time.h>

#define PAGE_SIZE 50
#define ID_LEN 24
#define EMAIL_LEN 64
#define MAX_FINE_CENTS 2500
#define MAX_BRANCHES 8
#define SECONDS_PER_DAY 86400

typedef struct {
    char id[ID_LEN];
    char member[ID_LEN];
    char email[EMAIL_LEN];
    time_t due;
} loan_t;

typedef struct {
    loan_t *items;
    size_t count;
    size_t cap;
} loan_list_t;

typedef struct {
    loan_t items[PAGE_SIZE];
    size_t count;
    int next_page; /* -1 when this is the last page */
} loan_page_t;

typedef int (*page_fetcher_fn)(void *ctx, int page, loan_page_t *out);

typedef struct {
    char api_token[64];
    char ledger_dir[256];
    int fee_cents_per_day;
    int grace_days;
    char branches[MAX_BRANCHES][32];
    size_t branch_count;
} lend_config_t;

/* config.c */
void config_load(lend_config_t *cfg);

/* loan_api.c */
void loan_list_init(loan_list_t *l);
int loan_list_push(loan_list_t *l, const loan_t *loan);
void loan_list_free(loan_list_t *l);
int loan_api_fetch_all(page_fetcher_fn fetch, void *ctx, loan_list_t *out);
int loan_api_file_fetcher(void *ctx, int page, loan_page_t *out);

/* fines.c */
int fine_for_loan(const loan_t *l, time_t now, const lend_config_t *cfg);
int collect_overdue(const loan_list_t *all, time_t now, const lend_config_t *cfg,
                    loan_list_t *overdue, long *total_cents);

/* ledger.c */
size_t loan_list_dedupe(loan_list_t *l);
int ledger_count_member(const char *ledger_dir, const char *member, int *out);

#endif
