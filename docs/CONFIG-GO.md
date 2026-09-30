# Fuel reconciliation service configuration

The service reads these environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `FUELRECON_ADDR` | Address the HTTP API listens on. | `:8080` |
| `FUELRECON_API_TOKEN` | Bearer token for the fuel-card provider API. | none |
| `FUELRECON_PROVIDER_HOSTS` | Provider API hosts. The first entry is used. | `api.fuelcards.example` |
| `FUELRECON_PAGE_TIMEOUT` | How long to wait for each provider page request. | |

Example:

    FUELRECON_API_TOKEN=changeme
    FUELRECON_PROVIDER_HOSTS=api.fuelcards.example
