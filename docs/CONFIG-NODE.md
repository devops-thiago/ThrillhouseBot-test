# usage-rollup configuration

Every setting is read once at start-up by `src/config.js`. A value that cannot be parsed fails the
start-up rather than being silently replaced by its default.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `USAGE_EVENTS_URL` | yes | — | Base URL of the metering pipeline's event feed, without a trailing slash. The service appends `/v1/usage-events`. |
| `USAGE_EVENTS_TOKEN` | yes | — | Bearer token for the feed. Issued per environment by the metering team and delivered as a deployment secret. |
| `BILLABLE_METRICS` | no | `api_requests,storage_gb_hours,egress_gb` | Metric names that may appear on an invoice. Anything else in the feed is counted and dropped. |
| `ROLLUP_INTERVAL_SECONDS` | no | `300` | Seconds between scheduled rollups. One runs immediately at start-up. |
| `LATE_EVENT_GRACE_HOURS` | no | `48` | How long after a period ends its rollup keeps being recomputed, to pick up events the meters buffered across the boundary. |
| `ROLLUP_STATE_FILE` | no | `/var/lib/usage-rollup/rollups.json` | Where the computed snapshots are written. The directory is created if it does not exist. |
| `PORT` | no | `8080` | HTTP listen port. |

## Endpoints

| Method | Path | Returns |
| --- | --- | --- |
| `GET` | `/v1/rollups` | The period ids currently held in the store |
| `GET` | `/v1/rollups/{period}` | The stored snapshot for one period: summary counters and one line per tenant and metric |
| `GET` | `/v1/rollups/{period}/tenants/{tenantId}` | Just that tenant's lines |
| `POST` | `/v1/rollups/{period}/refresh` | Recomputes one period from the feed, stores it and returns it |
| `GET` | `/healthz` | Liveness, plus the periods held in the store |

`{period}` is always `YYYY-MM` in UTC; a value in any other shape is answered with `400`.

The refresh endpoint accepts any period, including a closed one, which is how a correction is
applied after billing has queried a month. The scheduled run only touches periods that are still
open — the current one, and the previous one until `LATE_EVENT_GRACE_HOURS` has elapsed.

## Quantities

Meters report fractional quantities and the accumulator sums them as integer thousandths, so a
month of readings does not drift. A line carries both the summed `quantity` and `billableUnits`,
which rounds a partial unit up because that is what the contracts say.
