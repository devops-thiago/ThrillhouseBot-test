# gcompact configuration

`gcompact` reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `GCOMPACT_HUB_URL` | Base URL of the greenhouse sensor hub API. | `http://sensor-hub.greenhouse.local` |
| `GCOMPACT_SENSOR_IDS` | Sensors whose readings are pulled and compacted each cycle, e.g. `gh-01`. | `gh-01` |
| `GCOMPACT_WINDOW_SECONDS` | Width of each compaction window in seconds; also the pause between cycles. | `300` |
| `GCOMPACT_ARCHIVE_DIR` | Directory holding one CSV archive file per sensor. | `/var/lib/gcompact/archive` |

The hub credential is read from `GCOMPACT_API_TOKEN`; set it in every deployment.

Example `.env` for local runs:

```
GCOMPACT_HUB_URL=http://localhost:9000
GCOMPACT_SENSOR_IDS=gh-01
GCOMPACT_WINDOW_SECONDS=60
GCOMPACT_ARCHIVE_DIR=./archive
GCOMPACT_API_TOKEN=changeme
```
