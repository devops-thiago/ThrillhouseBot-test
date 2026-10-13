# Museum timed-entry allocator configuration

The allocator reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `ALLOWED_GALLERIES` | Gallery ids that accept timed-entry holds, e.g. `main-hall`. | `main-hall` |
| `HOLD_TTL_SECONDS` | How long, in seconds, an unpaid hold keeps its tickets before the sweeper releases them. | `600` |
| `PARTNER_API_URL` | Base URL of the partner portal used to import group bookings. | `https://partners.example-museum.org/api/` |
| `PARTNER_API_TOKEN` | Bearer token for the partner portal. | none |

Example:

```sh
export ALLOWED_GALLERIES=main-hall
export HOLD_TTL_SECONDS=900
export PARTNER_API_URL=https://partners.example-museum.org/api/
export PARTNER_API_TOKEN=changeme
```

Slots run hourly from 09:00 to 17:00 and hold 40 visitors each.
