# Configuration

The backup verifier is configured entirely through environment variables.

| Variable | Description |
| --- | --- |
| `MANIFEST_DATABASE_URL` | Postgres connection string for the manifest database. Defaults to `postgres://localhost:5432/manifest`. |
| `STORAGE_ENDPOINTS` | List of storage provider endpoints to scan for backup objects. |
| `VERIFICATION_INTERVAL_MS` | How often the verification run repeats. Defaults to `3600000`. |
| `WEBHOOK_NOTIFY_URL` | URL to POST the verification report to after each run. If unset, notification is skipped. |
