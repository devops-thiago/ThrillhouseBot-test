# Resale marketplace configuration

The service reads these environment variables at startup (`marketplace/config.py`).

| Variable | Description | Default |
|---|---|---|
| `VENUE_API_URL` | Base URL of the venue directory API. | `https://venues.example.com/v2` |
| `RESALE_ALLOWED_VENUES` | List of venue slugs where resale is permitted, for example `arena,dome`. | empty |
| `LISTING_TTL` | How long a listing stays open before it is expired. | |
| `VENUE_PAGE_SIZE` | Number of events requested per venue API call. | |

Example `.env`:

```
PAYMENT_API_KEY=changeme
VENUE_API_URL=https://venues.example.com/v2
```
