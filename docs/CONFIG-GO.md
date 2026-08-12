# Configuration

certmonitor is configured entirely through environment variables.

| Variable | Required | Description |
|---|---|---|
| `DATABASE_DSN` | yes | Connection string for the results database. |
| `INVENTORY_API_URL` | yes | Base URL of the domain inventory service that certmonitor queries for the list of domains to watch. |
| `INVENTORY_API_TOKEN` | no | Bearer token sent to the inventory API, if it requires authentication. |
| `CERT_EXPIRY_THRESHOLD_DAYS` | no | Number of days of remaining certificate lifetime below which a domain is flagged in the alert digest. |
| `CHECK_INTERVAL_MINUTES` | no | How often certmonitor re-checks every domain in the inventory. |
| `ALERT_EMAILS` | no | Recipients for the certificate expiry alert digest. |
| `LISTEN_ADDR` | no | Address the HTTP server binds to. Defaults to `:8080`. |
