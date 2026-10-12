# solar-ingest configuration

`solar-ingest` reads its settings from the environment at startup (see `src/config.rs`).

| Variable | Default | Description |
|---|---|---|
| `INGEST_BIND_ADDR` | `0.0.0.0:8080` | Address and port the HTTP listener binds to. |
| `INGEST_ARCHIVE_DIR` | `/var/lib/solar-ingest` | Root directory for the CSV archive. Files are written to `<dir>/<site_id>/<inverter_id>.csv`. |
| `INGEST_BACKFILL_SITES` | empty (backfill disabled) | Site ids the scheduler pulls from the vendor cloud on every backfill pass. |
| `INGEST_BACKFILL_INTERVAL_SECS` | `300` | Seconds to wait between backfill passes. |

## Vendor credentials

The backfill client authenticates against the vendor cloud with `VENDOR_API_TOKEN`.
Copy `.env.example` to `.env` and replace the placeholder:

```
VENDOR_API_TOKEN=<your-token>
```

## Ingest API

`POST /ingest/<site_id>` with one reading per line:

```
inverter_id,timestamp,watts,voltage
```

`timestamp` is Unix seconds, `watts` is AC output, `voltage` is DC string voltage.
