# Configuration

The drift detector is configured entirely through environment variables.

| Variable | Description |
|---|---|
| `REGISTRAR_BASE_URL` | Base URL of the registrar's REST API, e.g. `https://api.registrar.example.com`. |
| `REGISTRAR_API_KEY` | Bearer token used to authenticate against the registrar API. |
| `TRACKED_DOMAINS` | The domains the detector should monitor for drift. |
| `DATABASE_URL` | JDBC connection string for the database that stores detected drift entries. |
| `POLL_INTERVAL_SECONDS` | How often to poll the registrar for each tracked domain. Defaults to `300`. |
