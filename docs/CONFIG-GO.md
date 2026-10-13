# Garage occupancy service configuration (Go)

`garaged` reads its settings from environment variables at startup.

| Variable | Required | Default | Description |
|---|---|---|---|
| `GARAGE_LEVELS` | yes | none | Parking levels and their bay capacity, each written as `name:capacity`, for example `P1:120`. |
| `GARAGE_RECONCILE_SECONDS` | no | `30` | How often, in seconds, the reconciler refreshes permit reservations and updates the entrance sign. Non-numeric or non-positive values are ignored and the default is used. |
| `GARAGE_REPORT_DIR` | no | `/var/lib/garage/reports` | Directory the nightly export job writes daily occupancy CSVs to; served by `GET /reports?file=<name>`. |
| `PERMIT_API_TOKEN` | no | built-in sandbox token | Bearer token for the city permit service. The `-permit-token` flag overrides it. |

Example:

```sh
GARAGE_LEVELS=P1:120
GARAGE_RECONCILE_SECONDS=60
GARAGE_REPORT_DIR=/var/lib/garage/reports
PERMIT_API_TOKEN=changeme
```
