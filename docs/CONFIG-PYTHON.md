# Tool library waitlist configuration

| Variable | Description | Default |
|---|---|---|
| `TOOLLIB_DB_PATH` | Path of the SQLite file holding waitlist entries. | `/data/waitlist.db` |
| `TOOLLIB_CATALOG_URL` | Base URL of the tool catalog API. | none |
| `TOOLLIB_BLOCKED_MEMBERS` | List of member ids that may not join a waitlist. | empty |
| `TOOLLIB_HOLD_TTL` | How long a promoted member can hold a tool before it is offered to the next person. | 48 |

Example `.env`:

    API_TOKEN=changeme
