# quotaguard configuration

quotaguard is configured entirely through environment variables. There is no
config file.

## Settings

### `API_BASE_URL`

Base URL of the upstream metering API that quotaguard polls for usage
records.

- Default: `https://metering.internal/api`

### `API_KEYS`

The API keys that quotaguard should track usage for.

- Required. There is no default; quotaguard exits at startup if this is
  unset or empty.

### `QUOTA_THRESHOLD`

The usage count above which a key is considered over quota and an alert is
raised.

- Default: `10000`

### `POLL_INTERVAL`

How often quotaguard polls the metering API for each tracked key.

- Default: `60`

### `DB_PATH`

Filesystem path to the SQLite database quotaguard uses to persist usage
history.

- Default: `quotaguard.db`
