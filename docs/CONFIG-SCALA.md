# Cold-chain tracker configuration

| Variable | Description | Default |
|---|---|---|
| `COLDCHAIN_ALERT_RECIPIENTS` | Addresses that receive excursion alerts. | none |
| `COLDCHAIN_GRACE_PERIOD` | How long a reading may stay out of range before an excursion is raised. | |
| `COLDCHAIN_SENSOR_URL` | Base URL of the sensor API. | `http://sensors.internal:8080` |
| `COLDCHAIN_API_TOKEN` | Bearer token for the sensor API. | |

Example:

    COLDCHAIN_API_TOKEN=changeme
