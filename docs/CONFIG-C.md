# usersync configuration

`usersync` reads its configuration from environment variables. There is no
config file.

## `USERSYNC_API_BASE_URL`

Base URL of the directory API that `sync`, `find`, and `search` pull
records from.

Default: `https://directory.internal.example.com`

## `USERSYNC_ALLOWED_DOMAINS`

Restricts sync to users whose email address belongs to one of these
domains. Leave unset to allow every domain.

Example: `USERSYNC_ALLOWED_DOMAINS=example.com`

## `USERSYNC_TIMEOUT_MS`

Timeout applied to directory API requests.
