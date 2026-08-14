# costalloc configuration

All settings are read from the environment once at start-up by
`src/main/kotlin/com/thrillhouse/costalloc/Config.kt`.

| Variable | Meaning | Default |
| --- | --- | --- |
| `COSTALLOC_BILLING_API` | Base URL of the provider's billing API. | `https://billing.internal/v1` |
| `COSTALLOC_TEAM_TAG` | Resource tag key holding the owning team or cost-centre id. | `owner-team` |
| `COSTALLOC_INCLUDED_ACCOUNTS` | Accounts to allocate. Leave unset to allocate every account the API returns. | unset |
| `COSTALLOC_REFRESH_INTERVAL_SECONDS` | How often the scheduled refresh re-reads the usage export, in seconds. | `900` |

## The rate table

Amounts that are not already in `COSTALLOC_REPORTING_CURRENCY` are converted
through a rate table read from the file named by `COSTALLOC_RATES_FILE`
(default `/etc/costalloc/rates.properties`). The file is a plain
`CURRENCY=rate` properties file rotated daily by the deployment and mounted
into the container; the image does not ship one. A missing file leaves the
table empty, and any non-reporting currency then fails the conversion.

    EUR=1.08
    GBP=1.27

## Storage

Totals are written to the `team_costs` table over `COSTALLOC_JDBC_URL`. The
report endpoint reads them back; only the `/refresh` path talks to the billing
API.
