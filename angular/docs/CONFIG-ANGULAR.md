# Gym class booking board: configuration

The board reads its settings at runtime from `window.__env`, which `env.js` sets before the
app boots. Settings are parsed in `src/app/app-config.ts`. Missing or invalid values fall back
to the defaults below.

| Setting | Type | Default | Description |
|---|---|---|---|
| `BOOKING_API_URL` | string (URL or path) | `/api` | Base URL of the booking API. The bearer token is only attached to requests under this prefix. |
| `WAITLIST_TIERS` | list of strings | — | Membership tiers in waitlist priority order, highest first. Members in tiers not listed go to the back. |
| `HOLD_TIMEOUT_MINUTES` | integer, minutes | `15` | How long a freed spot is held for a promoted waitlist member before it is offered to the next one. |
| `MAX_BOOKINGS_PER_MEMBER` | integer | `3` | Members holding more bookings than this in the current week are flagged on the board. |

For local development, create `src/env.js` (it is copied into the build output when present):

```js
window.__env = {
  BOOKING_API_URL: 'http://localhost:8080/api',
  HOLD_TIMEOUT_MINUTES: '10',
};
```

The container proxies `/api/` to `BOOKING_API_UPSTREAM` and its health check calls `/api/health`
with the token in `BOOKING_API_TOKEN`:

```sh
docker run -e BOOKING_API_UPSTREAM=http://booking-api:8080/api/ \
  -e BOOKING_API_TOKEN=changeme -p 8080:80 gym-booking-board
```
