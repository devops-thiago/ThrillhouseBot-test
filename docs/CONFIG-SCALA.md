# Configuration

The event deduplicator is configured entirely through environment variables.

| Variable | Description | Default |
|---|---|---|
| `DEDUP_DB_URL` | JDBC connection string for the durable seen-events store. | `jdbc:postgresql://localhost:5432/events` |
| `UPSTREAM_API_BASE_URL` | Base URL of the upstream event history API used for backfill. | `https://events.internal.example.com` |
| `ALLOWED_SOURCES` | Event sources to backfill and monitor on startup. | `checkout,inventory,shipping` |
| `DUPLICATE_ALERT_THRESHOLD` | Number of duplicate events in a batch that triggers an on-call alert. | `5` |

## Notes

- `DEDUP_DB_URL` must point at a database with a `seen_events` table with a unique constraint
  on `event_id`.
- `UPSTREAM_API_BASE_URL` should not include a trailing slash.
- `DUPLICATE_ALERT_THRESHOLD` is compared against the number of duplicate events found in a
  single backfill batch per source.
