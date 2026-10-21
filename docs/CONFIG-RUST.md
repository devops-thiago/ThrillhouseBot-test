# Rail ingester configuration

The service reads these environment variables at start-up (see `rust/src/config.rs`).

| Variable | Description | Default |
|---|---|---|
| `RAIL_ZONES` | Track zones the ingester accepts readings for. | none |
| `RAIL_POLL_INTERVAL` | How often the sensor registry is polled. | `30` |
| `RAIL_ALERT_THRESHOLD_C` | Rail temperature in degrees Celsius at which a warning alert is raised. | `55.0` |
| `RAIL_API_TOKEN` | Token used to authenticate against the sensor registry. | none |

Example `.env`:

    RAIL_API_TOKEN=changeme
    RAIL_ZONES=north,south
