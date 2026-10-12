# meterval configuration

`meterval <zone>` reads its settings from the environment at startup.

| Variable | Required | Default | Description |
|---|---|---|---|
| `METER_API_URL` | no | `https://meters.internal/api/v1` | Base URL of the meter-data API. |
| `METER_API_TOKEN` | no | built-in staging token | Bearer token sent with every API request. |
| `METER_ALLOWED_ZONES` | yes | none | The distribution zones this instance validates. A zone passed on the command line must appear here. |
| `METER_ARCHIVE_DIR` | no | `/var/lib/meterval/archive` | Directory where rejected readings are appended, one CSV per meter. |

Example:

```sh
export METER_API_URL=https://meters.example.org/api/v1
export METER_API_TOKEN=<your-token>
export METER_ALLOWED_ZONES=north
meterval north
```

The meter registry is read from `/etc/meterval/registry.csv` (`serial,active` or
`serial,decommissioned` per line).

Exit codes: `0` all readings valid, `2` some readings rejected (archived), `64` usage or
configuration error, `69` registry, API or validation failure, `74` archive write failure.
