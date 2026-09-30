# Room booking configuration

| Variable | Description |
| --- | --- |
| `ROOMBOOK_ALLOWED_DOMAINS` | Organizer e-mail domains that may book rooms. Empty means everyone. |
| `ROOMBOOK_HOLD_TTL` | How long an unconfirmed booking is held before release. |
| `ROOMBOOK_PAGE_SIZE` | Page size used when reading events from the calendar API (default 50). |
| `CALENDAR_API_TOKEN` | Token for the calendar API. |

Example `.env`:

    CALENDAR_API_TOKEN=<your-token>
    ROOMBOOK_PAGE_SIZE=100
