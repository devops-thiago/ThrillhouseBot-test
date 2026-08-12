# logd configuration

`logd` is configured entirely through environment variables. All of them are
optional; sensible defaults are used for anything left unset.

## `LOGD_PORT`

TCP port `logd` listens on for incoming log connections. Defaults to `9000`.

## `LOGD_MAX_FILE_SIZE`

Maximum size, in bytes, that a single rotated log file is allowed to grow to
before `logd` rolls over to a new file.

## `LOGD_ALLOWED_SOURCES`

The set of source tags permitted to write logs. Any log line whose source is
not in this list is rejected.

## `LOGD_BAN_REFRESH_INTERVAL`

Controls how often `logd` refreshes the banned-IP list it pulls from the
admin API.
