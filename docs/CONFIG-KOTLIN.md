# Room booking service configuration

The service reads its settings from environment variables in `Config.kt`.

| Variable | Description | Default |
| --- | --- | --- |
| `ROOMS_ALLOWED_FLOORS` | Floors whose rooms may be booked. | none (no floor is bookable) |
| `HOLD_TTL` | How long an unconfirmed hold lasts before the sweeper drops it. | `900` |
| `CALENDAR_PAGE_SIZE` | Events requested per page from the calendar provider. | `50` |
| `CALENDAR_API_TOKEN` | Credential for the calendar provider. Set it to your own value, for example `CALENDAR_API_TOKEN=changeme`. | none |
