# Bicycle classifieds configuration

The service reads its settings from environment variables when it starts.

| Variable | Purpose | Default |
|---|---|---|
| `CLASSIFIEDS_DB_PATH` | Path to the SQLite database file. Use `:memory:` for throwaway runs. | `classifieds.db` |
| `CLASSIFIEDS_FRAME_SIZES` | Frame sizes a seller may choose when creating a listing. Values are case-insensitive. | All standard sizes, XS through XL |
| `CLASSIFIEDS_LISTING_TTL_DAYS` | Age in days after which `purge` deletes a listing. Must be at least 1. | `30` |
| `CLASSIFIEDS_PRICE_GUIDE_URL` | Base URL of the price guide API used for suggested prices. | `https://priceguide.example.com` |
| `API_TOKEN` | Bearer token for the price guide API. | none |

Example `.env` for local development:

```
CLASSIFIEDS_DB_PATH=./dev.db
CLASSIFIEDS_LISTING_TTL_DAYS=14
API_TOKEN=changeme
```
