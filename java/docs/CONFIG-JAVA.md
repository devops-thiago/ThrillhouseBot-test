# Waitlist service configuration

The service reads its settings from environment variables once at startup.

| Variable | Description | Default |
|---|---|---|
| `WAITLIST_DB_URL` | JDBC URL of the Postgres database holding the `waitlist` table. | `jdbc:postgresql://waitlist-db:5432/waitlist` |
| `SCHEDULER_BASE_URL` | Base URL of the central scheduling API, used for open slots and SMS relay. | `https://scheduler.clinic.internal/api/v2` |
| `WAITLIST_ALLOWED_CLINICS` | Clinic ids the background sweep reconciles every 60 seconds, e.g. `SP-101`. | empty (no sweep) |
| `WAITLIST_OFFER_TTL_SECONDS` | How long, in seconds, a slot is held for the notified patient before it expires. | `900` |

## Credentials

The scheduler token is injected from the deployment secret store:

```
SCHEDULER_API_TOKEN=<your-token>
```
