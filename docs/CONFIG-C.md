# Dispenser configuration

The dispenser reads these environment variables at startup (see `c/src/config.c`).

| Variable | Description |
|---|---|
| `BIKE_STATION_IDS` | Station names the dispenser accepts requests for, for example `harbor`, `central`. Requests for other stations are refused. |
| `CODE_TTL` | How long an unlock code stays valid after it is issued. |
| `MAX_CODES_PER_RIDER` | Number of unexpired codes one rider may hold at a time. |
| `AUDIT_DIR` | Directory holding `unlock.log`. Defaults to `/var/log/dispenser`. |

Example `.env`:

    UNLOCK_API_TOKEN=changeme
    BIKE_STATION_IDS=harbor
