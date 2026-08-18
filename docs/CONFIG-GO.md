# certwatchd configuration

`certwatchd` is configured entirely through environment variables, all prefixed
with `CERTWATCH_`. They are read once at start-up (`go/internal/config/config.go`),
so changing a value requires a restart.

## `CERTWATCH_DATABASE_URL`

PostgreSQL connection string for the database holding the `certificate_state`
and `certificate_waivers` tables. Required — the process exits at start-up when
it is unset or empty. There is no default.

## `CERTWATCH_SCAN_INTERVAL`

How often the scanner reconciles the service inventory with the certificates
the CA has issued. Accepts a Go duration string such as `10m`, `30m` or `4h`.
Default: `30m`. Unparseable or non-positive values fall back to the default.

## `CERTWATCH_WARN_DAYS`

Remaining lifetime, in whole days, below which an endpoint is reported at
`warning`. Default: `30`.

## `CERTWATCH_CRITICAL_DAYS`

Remaining lifetime, in whole days, below which an endpoint is reported at
`critical` instead of `warning`. Must not be larger than `CERTWATCH_WARN_DAYS`;
the process refuses to start if it is. Default: `7`.

## `CERTWATCH_SKIP_OWNERS`

Comma-separated list of owners whose endpoints are not evaluated at all — the
scan neither records state nor sends them a digest. Matched against the
inventory `owner` field case-insensitively, with surrounding whitespace
trimmed. Default: empty, meaning every owner is scanned.

## Related settings

`CERTWATCH_INVENTORY_URL` and `CERTWATCH_ISSUER_URL` point at the service
inventory and the internal CA; `CERTWATCH_API_TOKEN` is the bearer token used
for both. `CERTWATCH_DIGEST_URL` is the notification relay the per-owner
digests are posted to — leaving it empty turns delivery into a no-op, which is
how the service is run locally. `CERTWATCH_LISTEN_ADDR` sets the HTTP bind
address and defaults to `:8080`.

## Endpoints

| path | purpose |
| --- | --- |
| `GET /healthz` | liveness, plus the number of scans this process has run |
| `GET /report` | the summary of the most recent scan |
| `GET /findings?owner=…` | the recorded findings for one owner |
