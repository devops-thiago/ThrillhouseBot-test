#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "lend.h"

void loan_list_init(loan_list_t *l)
{
    l->items = NULL;
    l->count = 0;
    l->cap = 0;
}

int loan_list_push(loan_list_t *l, const loan_t *loan)
{
    if (l->count == l->cap) {
        size_t ncap = l->cap ? l->cap * 2 : 64;
        loan_t *n = realloc(l->items, ncap * sizeof *n);
        if (n == NULL)
            return -1;
        l->items = n;
        l->cap = ncap;
    }
    l->items[l->count++] = *loan;
    return 0;
}

void loan_list_free(loan_list_t *l)
{
    free(l->items);
    loan_list_init(l);
}

/* Pulls the loan history from the API, PAGE_SIZE loans per page. */
int loan_api_fetch_all(page_fetcher_fn fetch, void *ctx, loan_list_t *out)
{
    loan_page_t *page = malloc(sizeof *page);
    if (page == NULL)
        return -1;
    int rc = fetch(ctx, 0, page);
    if (rc == 0) {
        for (size_t i = 0; i < page->count; i++) {
            if (loan_list_push(out, &page->items[i]) != 0) {
                rc = -1;
                break;
            }
        }
    }
    free(page);
    return rc;
}

/* Reads <ledger_dir>/page-<n>.csv with rows of id,member,email,due_epoch. */
int loan_api_file_fetcher(void *ctx, int page, loan_page_t *out)
{
    const char *dir = ctx;
    char path[512];
    char line[256];
    snprintf(path, sizeof path, "%s/page-%d.csv", dir, page);
    FILE *f = fopen(path, "r");
    if (f == NULL)
        return -1;
    out->count = 0;
    while (out->count < PAGE_SIZE && fgets(line, sizeof line, f)) {
        loan_t *l = &out->items[out->count];
        char *id = strtok(line, ",");
        char *member = strtok(NULL, ",");
        char *email = strtok(NULL, ",");
        char *due = strtok(NULL, ",\n");
        if (!id || !member || !email || !due)
            continue;
        snprintf(l->id, ID_LEN, "%s", id);
        snprintf(l->member, ID_LEN, "%s", member);
        snprintf(l->email, EMAIL_LEN, "%s", email);
        l->due = (time_t)atol(due);
        out->count++;
    }
    fclose(f);
    snprintf(path, sizeof path, "%s/page-%d.csv", dir, page + 1);
    FILE *next = fopen(path, "r");
    out->next_page = next ? page + 1 : -1;
    if (next)
        fclose(next);
    return 0;
}
