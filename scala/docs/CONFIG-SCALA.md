# Baggage transfer tracker configuration

The service reads its settings from environment variables at startup (see `Config.fromEnv`).

| Variable | Default | Description |
|---|---|---|
| `TRANSFER_HUBS` | none (no hubs served) | Hubs where we handle transfers, each written as `HUB:minutes` with the hub's minimum connection time in minutes, e.g. `FRA:45`. A bag connecting through a hub not listed here is reported as at risk. |
| `RECONCILE_INTERVAL_SECONDS` | `60` | How often, in seconds, every outbound flight's manifest is reconciled against the scan log. |
| `MANIFEST_API_URL` | `http://localhost:8081/manifests` | Base URL of the airline manifest API. |
| `BAGGAGE_API_TOKEN` | built-in development token | Bearer token sent to the manifest API. Supply it from your secret store in every deployed environment. |

Example `.env` for local runs:

```
TRANSFER_HUBS=FRA:45
RECONCILE_INTERVAL_SECONDS=30
MANIFEST_API_URL=http://localhost:8081/manifests
BAGGAGE_API_TOKEN=changeme
```

## Endpoints

- `POST /scans` with body `tag,hub,outboundFlight` records a scan.
- `GET /transfers` lists the latest reconciled transfers.
- `GET /bags/{tag}/label` returns the printed label for a bag.
