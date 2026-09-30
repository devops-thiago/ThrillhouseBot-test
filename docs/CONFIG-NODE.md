# Room booking service configuration

The service reads its settings from environment variables in `node/src/config.js`.

| Variable | Description |
| --- | --- |
| `PORT` | TCP port the HTTP server listens on. Defaults to `8080`. |
| `ROOM_IDS` | The room identifiers that can be booked, for example `room-1,room-2`. |
| `HOLD_TIMEOUT` | How long a booking stays held before it is released if nobody confirms it. |
| `MAX_BOOKINGS_PER_USER` | Maximum number of active bookings one user may own. |

Example `.env` for local development:

```
CALENDAR_API_TOKEN=changeme
PORT=8080
```
