# Configuration

The export worker reads its configuration from environment variables.

| Variable | Description |
|---|---|
| `RECORDS_API_URL` | Base URL of the internal Records API. Required. |
| `EXPORT_OUTPUT_DIR` | Directory export bundles are written to. Defaults to `/data/exports`. |
| `EXPORT_DB_PATH` | Path to the SQLite request-tracking database. Defaults to `/data/requests.db`. |
| `RECORDS_API_TIMEOUT` | Timeout for Records API requests. |
