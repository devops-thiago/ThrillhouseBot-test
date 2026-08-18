# Configuration — invoice dunning worker

The worker reads its configuration from the process environment once, at
start-up (`dunning/config.py`). It runs one cycle and exits, so the CronJob
picks up a change on the next run.

| Variable | Default | Description |
|---|---|---|
| `BILLING_API_URL` | — | Base URL of the billing service. Required; the worker exits with status 2 without it. |
| `DELIVERY_API_URL` | — | Base URL of the delivery service the notifications are handed to. Required. |
| `DUNNING_API_TOKEN` | empty | Bearer token for both services. |
| `DUNNING_DB_PATH` | `/var/lib/dunning/dunning.db` | SQLite file holding the per-invoice dunning history. |
| `DUNNING_SCHEDULE` | see below | The dunning ladder. |
| `DUNNING_MIN_GAP_DAYS` | `2` | Minimum number of days between two notifications for the same invoice, whatever the ladder says. |
| `DUNNING_BATCH_LIMIT` | `2000` | Upper bound on how many invoices one cycle acts on. |
| `DUNNING_DRY_RUN` | unset | `1`, `true` or `yes` walks the invoices and logs what would go out without sending or recording anything. |

Values that are not whole non-negative numbers fall back to their default,
apart from `DUNNING_SCHEDULE`: a malformed ladder stops the worker rather than
quietly reverting to the default, because the wrong ladder means the wrong
letters reach real customers.

## `DUNNING_SCHEDULE`

Comma-separated `days:name[:channel]` entries, in any order. `days` is how long
an invoice has to be overdue before that stage is due, `name` selects the
`dunning.<name>` template in the delivery service, and `channel` is what the
delivery service should use — `+` separates several. The channel `none` marks
the hand-over stage: nothing is sent to the customer, the invoice is recorded as
handed to collections.

Default:

```
3:reminder:email,10:notice:email,21:final_notice:email+sms,35:handover:none
```

Two stages may not share the same age, and a stage name may only appear once —
the ladder position is what stops an invoice being chased twice for the same
rung.

## Invoices the cycle never chases

Invoices whose billing state is `payment_plan`, `disputed` or `write_off` are
counted in the summary as `excluded_state` and skipped. That list lives in
`dunning/billing.py` because it mirrors the billing service's own vocabulary.
