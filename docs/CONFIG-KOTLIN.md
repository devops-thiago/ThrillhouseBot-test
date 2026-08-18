# meterfold configuration

Settings are read from the environment once at start-up by
`src/main/kotlin/com/thrillhouse/meterfold/Settings.kt`.

| Variable | Meaning | Default |
| --- | --- | --- |
| `METERFOLD_JDBC_URL` | Metering database holding `usage_event` and `usage_rollup`. | `jdbc:postgresql://localhost:5432/metering` |
| `METERFOLD_HTTP_PORT` | Listen port for the billing API. | `8080` |
| `METERFOLD_RECALCULATE_INTERVAL_MINUTES` | Minutes between scheduled recalculations. The first one runs at start-up. | `10` |
| `METERFOLD_LATE_ARRIVAL_GRACE_HOURS` | How long into a new period the previous one keeps being recalculated. | `72` |
| `METERFOLD_PRICE_BOOK` | Path to the mounted price book. | `/etc/meterfold/price-book.properties` |
| `METERFOLD_DEFAULT_UNIT_CENTS` | Unit price used for a meter that is listed in the price book without one. | `0` |

## The price book

A properties file keyed `<meter>.<setting>`, mounted by the deployment and reloaded only on
restart. A missing file fails the start-up: running with no prices would quietly bill everybody
zero.

    api_requests.unit_cents=2
    api_requests.included_units=100000
    storage_gb_hours.unit_cents=1
    storage_gb_hours.minimum_cents=500
    egress_gb.unit_cents=9

| Setting | Meaning |
| --- | --- |
| `unit_cents` | Price of one chargeable unit. |
| `included_units` | Units covered by the subscription before anything is charged. |
| `minimum_cents` | Floor applied once the meter is used at all, regardless of the allowance. |

A meter that does not appear in the price book is not billable and never reaches a rollup line. The
metering pipeline carries diagnostic meters on the same table, and leaving them out of the book is
how they stay off invoices.

## Endpoints

| Method | Path | Returns |
| --- | --- | --- |
| `GET` | `/billing/periods` | Period ids that have a stored rollup |
| `GET` | `/billing/periods/{period}` | The stored rollup: one line per tenant and meter, plus the period total |
| `GET` | `/billing/periods/{period}/tenants/{tenant}` | One tenant's lines and total |
| `POST` | `/billing/periods/{period}/recalculate` | Recalculates the period from the event table and returns it |
| `GET` | `/health` | Liveness |

`{period}` is `YYYY-MM` in UTC; anything else is answered with `400`. Quantities are rendered as
decimal strings rather than JSON numbers, because the finance importer parses them as decimals and
a JSON number would reach it as a double.

## Tables

`usage_event` is written by the ingest service; this one only reads it, and relies on its unique
index on `event_id` for deduplication. `usage_rollup` is owned here and rewritten wholesale, per
period, inside a transaction.
