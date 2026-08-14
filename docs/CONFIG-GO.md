# rotationd configuration

`rotationd` is configured entirely through environment variables, all prefixed
with `ROTATION_`. The service reads them once at start-up (see
`go/internal/config/config.go`); changing a value requires a restart.

## `ROTATION_DATABASE_URL`

PostgreSQL connection string for the database holding the `secret_rotation`
table. Required — the process exits at start-up when it is unset or empty.
There is no default.

## `ROTATION_SWEEP_INTERVAL`

How often the background sweeper reconciles the vault inventory with the
recorded rotation history. Accepts a Go duration string such as `90s`, `15m` or
`2h`. Default: `5m`. Unparseable values fall back to the default.

## `ROTATION_MAX_AGE_DAYS`

Number of days a secret may go without being rotated before it is reported as
overdue. Must be a positive whole number of days; unparseable or non-positive
values fall back to the default. Default: `90`.

## `ROTATION_EXEMPT_OWNERS`

Owners whose secrets are still tracked and stored but are never evaluated
against the age policy — useful for frozen systems whose credentials cannot be
rotated yet. Values are matched against the `owner` field exactly, and
surrounding whitespace is trimmed. Default: empty, meaning no owner is exempt.

## Related settings

`ROTATION_VAULT_URL`, `ROTATION_VAULT_TOKEN` and `ROTATION_WEBHOOK_URL` point
the service at the vault namespace and the operator relay.
`ROTATION_LISTEN_ADDR` sets the HTTP bind address and defaults to `:8080`.
