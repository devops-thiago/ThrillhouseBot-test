# Room booking service configuration

| Variable | Description |
|---|---|
| `ROOMBOOK_ADDR` | Listen address, default `:8080`. |
| `ROOMBOOK_ALLOWED_ROOMS` | List of room IDs that may be booked. |
| `ROOMBOOK_HOLD_TTL` | How long a tentative hold lasts before it is dropped. |
| `ROOMBOOK_PLANS_DIR` | Directory containing floor plan files. |

Set the calendar credential with `ROOMBOOK_API_TOKEN=changeme`.
