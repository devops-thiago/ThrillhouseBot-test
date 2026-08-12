# FlagSync configuration

FlagSync reads its configuration entirely from environment variables. There
is no config file.

## `REMOTE_CONFIG_URL`

Base URL of the remote flag-config service that FlagSync syncs flag
definitions from. Default: `https://config.internal.example.com`.

## `FLAGSYNC_DB_URL`

JDBC connection string for the audit database. Default:
`jdbc:postgresql://localhost:5432/flagsync`.

## `FLAG_SYNC_INTERVAL_SECONDS`

How often the background sync loop runs, in seconds. Default: `60`.

## `FLAG_SYNC_ENVIRONMENTS`

Which environments to sync flags for. Default: `prod`.
