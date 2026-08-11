# Configuration

`logtrail` reads the following environment variables at startup. All are
optional.

| Variable | Description |
|---|---|
| `LOGTRAIL_DATA_DIR` | Directory where validated records are written. Defaults to `data`. |
| `LOGTRAIL_MAX_LINES` | Maximum number of log lines processed per `ingest` run. |
| `LOGTRAIL_ALLOWED_EXT` | File extensions considered during ingest, e.g. `.log`. |
| `LOGTRAIL_RULES_TIMEOUT` | Timeout applied when syncing validation rules from the remote rules service. |

`LOGTRAIL_DATA_DIR` should point at a writable path; `logtrail` creates one
record file per ingested log line under it during `ingest`.
