# Configuration

The dispatcher is configured entirely through environment variables. There
is no config file.

| Variable | Description |
|---|---|
| `WEBHOOKRELAY_PORT` | Port the HTTP API listens on. Defaults to `8080`. |
| `WEBHOOKRELAY_DIRECTORY_URL` | Base URL of the subscriber directory service used to look up who should receive events. Required. |
| `WEBHOOKRELAY_DISPATCH_TIMEOUT` | How long to wait for a subscriber's endpoint to respond before treating the delivery as failed, expressed as a Go duration string (e.g. `5s`, `2m`). Defaults to `5s`. |
| `WEBHOOKRELAY_MAX_RETRIES` | Maximum number of delivery attempts for a single event before giving up. Defaults to `3`. |
| `WEBHOOKRELAY_ALLOWED_TOPICS` | Restricts dispatch to specific event topics, e.g. `order.created`. When unset, all topics are dispatched. |

`WEBHOOKRELAY_DATABASE_URL` points the delivery log store at its Postgres
instance and is required in any environment that exposes the `/subscribers/logs`
endpoint.
