# linkc configuration

`linkc` is configured entirely through environment variables. There is no
config file.

## `LINKC_PORT`

TCP port the service listens on.

- Type: integer
- Default: `8080`

## `LINKC_DB_PATH`

Filesystem path to the sqlite database file used to persist short code to
URL mappings. The file is created on first startup if it does not exist.

- Type: string (path)
- Default: `./linkc.db`

## `LINKC_DENYLIST_API_URL`

Base URL of the moderation service's banned-domains API, queried once on
startup to populate the in-memory denylist used to reject shortening
requests for banned domains.

- Type: string (URL)
- Default: `https://moderation.internal/api/domains`

## `LINKC_EXTRA_DENYLIST`

Additional domains to always treat as banned, checked on top of whatever
the moderation API returns. Useful for blocking a domain immediately
without waiting on the moderation service to catch up.

- Type: string
