# Customs hold tracker configuration

The service reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `CUSTOMS_BROKER_URL` | Base URL of the customs broker API used for hold sync and release notices. | `https://broker.example.internal` |
| `CUSTOMS_SYNC_INTERVAL` | How often holds are pulled from the broker. | `300` |
| `CUSTOMS_EXEMPT_REASONS` | Comma-separated hold reason codes ignored during broker sync, e.g. `VGM_PENDING,DOC_REVIEW`. | empty (nothing exempt) |
| `CUSTOMS_ARCHIVE_DIR` | Directory where closed-hold manifest bundles are written. | `/var/lib/customs/archive` |

## Credentials

The broker token is supplied as `CUSTOMS_API_TOKEN`. For local runs put it in an env file:

```
CUSTOMS_BROKER_URL=https://broker.example.internal
CUSTOMS_API_TOKEN=changeme
```

and start the container with `docker run --env-file customs.env -p 8080:8080 customs-hold-tracker`.
