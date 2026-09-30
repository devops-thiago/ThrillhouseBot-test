#include <stdio.h>
#include <string.h>
#include <time.h>
#include "lend.h"
#include "notifier.h"

int main(int argc, char **argv)
{
    lend_config_t cfg;
    loan_list_t loans, overdue;
    long total = 0;
    size_t failed = 0;

    config_load(&cfg);
    loan_list_init(&loans);
    loan_list_init(&overdue);

    if (loan_api_fetch_all(loan_api_file_fetcher, cfg.ledger_dir, &loans) != 0) {
        fprintf(stderr, "lendctl: cannot read loans from %s\n", cfg.ledger_dir);
        return 2;
    }
    loan_list_dedupe(&loans);
    if (collect_overdue(&loans, time(NULL), &cfg, &overdue, &total) != 0)
        return 2;

    printf("loans=%zu overdue=%zu fines_cents=%ld\n", loans.count, overdue.count, total);
    if (overdue.count > 0) {
        size_t sent = notify_overdue(&overdue, notify_stdout, &failed);
        printf("notices sent=%zu failed=%zu\n", sent, failed);
    }
    if (argc == 3 && strcmp(argv[1], "--member") == 0) {
        int n = 0;
        if (ledger_count_member(cfg.ledger_dir, argv[2], &n) == 0)
            printf("member %s has %d loans\n", argv[2], n);
    }
    loan_list_free(&loans);
    loan_list_free(&overdue);
    return 0;
}
