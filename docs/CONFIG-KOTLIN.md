# Configuration

The notification scheduler is configured entirely through environment
variables, read once at startup by `SchedulerConfig.fromEnv`.

| Variable | Required | Description |
|---|---|---|
| `NOTIFY_DB_URL` | yes | JDBC connection string for the notifications database. |
| `NOTIFY_DELIVERY_API_BASE_URL` | yes | Base URL of the downstream delivery provider's HTTP API. |
| `NOTIFY_CHANNELS` | no | Which delivery channels this instance processes. |
| `NOTIFY_POLL_INTERVAL_SECONDS` | no | How often the scheduler checks for pending notifications. |
| `NOTIFY_MAX_RETRY_ATTEMPTS` | no | Number of times to retry a failed delivery before marking it FAILED. Default: `3`. |
