# Configuration

The webhook delivery service is configured entirely through environment variables.

| Variable | Description | Default |
| --- | --- | --- |
| `WEBHOOK_DB_PATH` | Path to the SQLite database file holding subscriber records. | `subscribers.db` |
| `WEBHOOK_QUEUE_URL` | Base URL of the internal event queue service that events are pulled from. | `http://event-queue.internal` |
| `WEBHOOK_ALLOWED_HOSTS` | Comma-separated list of hostnames that subscriber endpoint URLs are allowed to target. When unset, no host filtering is applied. | (none) |
| `WEBHOOK_DELIVERY_TIMEOUT` | Request timeout applied to each delivery attempt. | `5` |

## Notes

- `WEBHOOK_LOG_LEVEL` controls the root logger level (e.g. `INFO`, `DEBUG`) and defaults to `INFO`.
- Subscribers are read from the `subscribers` table; there is currently no admin API for creating
  them, only the internal CLI.
