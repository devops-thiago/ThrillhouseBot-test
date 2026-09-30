# Roombook configuration

| Variable | Description | Default |
|---|---|---|
| `ROOMBOOK_API_TOKEN` | Token used to authenticate calls to the room directory. | none |
| `ROOMBOOK_ALLOWED_DOMAINS` | Organizer email domains that may create bookings, for example `corp.io`. Empty allows all. | empty |
| `ROOMBOOK_HOLD_TTL` | How long a tentative hold lasts before the reaper removes it. | 900 |
| `ROOMBOOK_PAGE_SIZE` | Rooms requested per directory page. | |

Example:

    ROOMBOOK_API_TOKEN=changeme
