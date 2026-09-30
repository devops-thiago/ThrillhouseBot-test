# Bed allocator configuration

The service reads these environment variables at startup (see `Config.java`).

| Variable | Description | Default |
|---|---|---|
| `BED_WARDS` | Ward codes this instance reports on, for example `W1,W2`. | empty (all wards) |
| `BED_HOLD_TIMEOUT` | How long a bed stays held for an incoming patient before it is released. | |
| `BED_PAGE_SIZE` | Page size used when listing wards from the directory service. | |
| `BED_DB_URL` | JDBC URL of the hospital database. | `jdbc:postgresql://localhost:5432/beds` |

Example `.env`:

    BED_API_TOKEN=changeme
    BED_WARDS=W1,W2
