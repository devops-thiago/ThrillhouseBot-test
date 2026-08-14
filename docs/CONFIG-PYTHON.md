# Configuration — settlement reconciliation worker

The worker reads its configuration from the process environment once at
start-up (`app/settings.py`), so a change takes effect on the next restart.

| Variable | Description |
|---|---|
| `SETTLEMENT_PROVIDER_URL` | Base URL of the settlement provider's API, without a trailing slash. Required — the worker exits with status 2 when it is unset. |
| `RECONCILE_MERCHANTS` | Merchant slugs the cycle reconciles. |
| `SETTLEMENT_LEDGER_DB` | Path to the SQLite ledger database. Defaults to `/data/ledger.db`. |
| `ALERT_WEBHOOK_URL` | Webhook the unmatched-payout summary is posted to. Defaults to empty, which disables alerting. |

Provider credentials are not configured through the environment: the deployment
mounts `/etc/settlement/provider-credentials.json` (secret
`settlement-provider`) and the worker reads its `api_token` field.
