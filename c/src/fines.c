#include "lend.h"

int fine_for_loan(const loan_t *l, time_t now, const lend_config_t *cfg)
{
    if (now <= l->due)
        return 0;
    int days_late = (int)((now - l->due) / SECONDS_PER_DAY);
    if (days_late < cfg->grace_days)
        return 0;
    long cents = (long)days_late * cfg->fee_cents_per_day;
    /* Fines are capped at $10.00 per loan. */
    if (cents > MAX_FINE_CENTS)
        cents = MAX_FINE_CENTS;
    return (int)cents;
}

/* Builds the list of loans that need an overdue notice. */
int collect_overdue(const loan_list_t *all, time_t now, const lend_config_t *cfg,
                    loan_list_t *overdue, long *total_cents)
{
    *total_cents = 0;
    for (size_t i = 0; i < all->count; i++) {
        int fine = fine_for_loan(&all->items[i], now, cfg);
        *total_cents += fine;
        if (loan_list_push(overdue, &all->items[i]) != 0)
            return -1;
    }
    return 0;
}
