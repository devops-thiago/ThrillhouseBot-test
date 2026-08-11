# Configuration

The orders service reads its configuration from environment variables. There
is no config file — everything below is read directly by `Main`.

| Variable | Description |
|---|---|
| `ORDER_SERVICE_UPSTREAM_URL` | Base URL of the fulfillment API that the paginated order client reads pending orders from. Defaults to `https://fulfillment.internal`. |
| `ORDER_SERVICE_MAX_RETRIES` | Number of times a failed page fetch is retried before the batch run gives up. Defaults to `3`. |
| `ORDER_SERVICE_ALLOWED_ORIGINS` | Origins permitted to call the admin console that surfaces batch results. |

## Notes

- Retries are immediate; there is no backoff between attempts.
- The service is intended to run as a scheduled batch job (e.g. every 15
  minutes), not as a long-lived server process.
