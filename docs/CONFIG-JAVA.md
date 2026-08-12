# Configuration

The download-stats aggregator is configured entirely through environment
variables; there is no configuration file.

| Variable | Description | Required | Default |
|---|---|---|---|
| `REGISTRY_API_BASE_URL` | Base URL of the package registry's stats API, e.g. `https://registry.example.com/api`. | Yes | — |
| `REGISTRY_PACKAGE_NAMES` | Comma-separated list of package names to track, e.g. `left-pad,right-pad`. | Yes | — |
| `REGISTRY_REQUEST_TIMEOUT` | Timeout applied to each request made to the registry API. | No | — |
| `STATS_DB_PATH` | Filesystem path to the local SQLite database file used to store aggregated stats. | No | `stats.db` |

## Notes

- `REGISTRY_API_BASE_URL` should not include a trailing slash.
- The aggregator exits non-zero if any tracked package could not be
  aggregated, so it is safe to run on a schedule and alert on a non-zero
  exit code.
