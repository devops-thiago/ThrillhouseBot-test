# Refill queue configuration

The Java refill queue service (`java/`) reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `REFILL_STORE_CODES` | Store codes this instance serves. Patient history lookups for any other store are rejected with 400. | none (no stores) |
| `AUTO_REFILL_INTERVAL` | How often the auto-refill job checks enrolled prescriptions, in minutes. | `15` |
| `INSURER_API_TOKEN` | Bearer token for the insurer formulary API. | sandbox token |
| `PHARMACY_DB_URL` | JDBC URL of the pharmacy management system database. | `jdbc:postgresql://localhost:5432/pharmacy` |

Example for local development:

```sh
export REFILL_STORE_CODES=ST01
export AUTO_REFILL_INTERVAL=5
export INSURER_API_TOKEN=changeme
export PHARMACY_DB_URL=jdbc:postgresql://localhost:5432/pharmacy
```
