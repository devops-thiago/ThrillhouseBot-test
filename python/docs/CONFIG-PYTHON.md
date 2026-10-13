# Garden waitlist configuration

The service reads its settings from environment variables at start-up
(see `garden_waitlist/config.py`).

| Variable | Meaning | Default |
|---|---|---|
| `GARDEN_DB_PATH` | Path of the SQLite database file. | `waitlist.db` (`/data/waitlist.db` in the container) |
| `GARDEN_PARKS_API_URL` | Base URL of the city parks plot registry. | `https://parks.example.org/api` |
| `GARDEN_BOROUGH_PREFIXES` | Postcode prefixes that count as in-borough for offer rounds, for example `E1`. | |
| `GARDEN_OFFER_TTL_HOURS` | How long a plot offer stays open, in hours. | `72` |

The parks registry key is passed separately:

```sh
GARDEN_PARKS_API_TOKEN=changeme
```
