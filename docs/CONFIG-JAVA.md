# Configuration

The suppression sync service is configured entirely through environment
variables. All are read once at startup in `Main`.

| Variable | Required | Description |
|---|---|---|
| `SUPPRESSION_PROVIDER_BASE_URL` | yes | Base URL of the email provider's API, e.g. `https://api.provider.example`. Used for both the suppression list endpoint and the metrics endpoint. |
| `SUPPRESSION_PROVIDER_API_KEY` | yes | Bearer token sent as the `Authorization` header on every provider request. |
| `CAMPAIGN_BATCH_SIZE` | no | Number of recipients handed to the delivery worker per batch. |
| `SUPPRESSION_SYNC_TIMEOUT` | no | Connect timeout used for calls to the provider. Defaults to `5000`. |

Startup fails fast with `IllegalStateException` if either required variable
is missing or blank.
