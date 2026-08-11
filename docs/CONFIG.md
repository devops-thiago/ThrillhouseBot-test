# Configuration

`logbeacon` is configured entirely through environment variables. There is no
config file.

| Variable | Description |
|---|---|
| `DATABASE_URL` | Path to the SQLite database file used to persist events. Defaults to `logbeacon.sqlite3` in the working directory. |
| `GITHUB_REPO` | The `owner/repo` slug the sync poller checks for existing issues before a critical event is escalated. |
| `GITHUB_TOKEN` | Personal access token used to authenticate requests to the GitHub API. |
| `CORS_ALLOWED_ORIGINS` | Origins permitted to make cross-origin requests to the API. |
| `POLL_INTERVAL_SECS` | How often, in seconds, the GitHub sync poller runs. Defaults to `300`. |
