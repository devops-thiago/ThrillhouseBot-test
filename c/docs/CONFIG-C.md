# Parcel locker PIN dispenser: configuration

The daemon reads its settings from the environment at startup.

| Variable | Required | Default | Description |
|---|---|---|---|
| `CARRIER_API_URL` | yes | none | Base URL of the carrier API, without a trailing slash. |
| `LOCKER_SITES` | no | `main` | Comma-separated list of site IDs to poll for parcels, at most 16. |
| `LOCKER_COUNT` | no | `24` | Number of lockers in the bank, between 1 and 20000. |
| `PIN_TTL` | no | `2880` | How long a dispensed PIN stays valid before the locker is released. |

The carrier API bearer token is read from `CARRIER_API_TOKEN`.

Example `.env` for local runs:

```
CARRIER_API_URL=https://carrier.example.com
CARRIER_API_TOKEN=changeme
LOCKER_SITES=main,annex
LOCKER_COUNT=48
```
