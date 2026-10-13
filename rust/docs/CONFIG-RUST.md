# turbine-alerts configuration

The ingester reads its settings from the environment once at startup. It listens on
port 8085 and loads per-sensor gains from `/etc/turbine-alerts/calibration.csv`
(mount it into the container; one `sensor_id,gain` line per sensor).

| Variable | Default | Description |
|---|---|---|
| `VIB_REGISTRY_ADDR` | `registry.internal:8080` | `host:port` of the fleet registry used to resolve turbine sites. |
| `API_TOKEN` | built-in development token | Bearer token sent to the fleet registry. |
| `VIB_ALERT_THRESHOLDS` | `4.5,7.1,11.2` | Comma-separated zone boundaries for RMS velocity in mm/s (ISO 10816 class II). Turbines at or above the second boundary page on-call. |
| `VIB_SWEEP_INTERVAL` | `60` | Interval between sweeps that re-page turbines still in the worst zone. |

Example `.env` for local runs:

```
VIB_REGISTRY_ADDR=localhost:8080
API_TOKEN=changeme
VIB_ALERT_THRESHOLDS=4.5,7.1,11.2
VIB_SWEEP_INTERVAL=30
```
