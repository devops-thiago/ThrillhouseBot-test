# labq configuration

`labq` reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `LABQ_ALLOWED_SITES` | Comma-separated list of collection site codes whose LIMS orders are accepted, e.g. `main,north`. | `main` |
| `LABQ_EXPORT_DIR` | Directory where rack manifests (`<rack>.csv`) are written. | `/var/lib/labq/manifests` |
| `LABQ_STALE_AFTER` | How long a sample may wait in the queue before `/status` counts it as stale. | `90` |
| `LABQ_API_TOKEN` | Bearer token for the LIMS API. Set this in every deployment. | built-in development token |

Example `.env` for local runs:

```
LABQ_ALLOWED_SITES=main,north
LABQ_EXPORT_DIR=./manifests
LABQ_STALE_AFTER=45
LABQ_API_TOKEN=changeme
```
