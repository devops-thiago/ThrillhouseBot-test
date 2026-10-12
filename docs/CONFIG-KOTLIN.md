# Newsletter billing service — configuration

The Kotlin billing service (`kotlin/`) reads its settings from environment variables once at startup
(`kotlin/src/main/kotlin/newsletter/billing/Config.kt`).

| Variable | Default | Description |
|---|---|---|
| `BILLING_SUPPORTED_CURRENCIES` | `USD` | Currencies the service will charge in. Subscribers billed in any other currency are reported as failed charges. |
| `BILLING_RUN_INTERVAL_MINUTES` | `60` | How often, in minutes, the scheduled billing cycle runs for the current month. |
| `BILLING_PORT` | `8080` | TCP port for the admin HTTP API (`POST /billing/run`, `GET /subscribers`). |
| `PAYMENT_API_TOKEN` | built-in development token | Bearer token sent to the payment gateway. Must be set in production. |

Other connection settings (`BILLING_JDBC_URL`, `PAYMENT_BASE_URL`, `ESP_BASE_URL`, `ESP_LIST_ID`) default to the
local development endpoints listed in `Config.kt`.

## Example `.env`

```
BILLING_RUN_INTERVAL_MINUTES=30
BILLING_PORT=8080
PAYMENT_API_TOKEN=changeme
```
