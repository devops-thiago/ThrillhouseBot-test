# Secret rotation auditor — configuration

Everything is read from the environment at start-up; there is no config file
to edit. `ROTATION_SECRETS_URL` and `ROTATION_TICKETS_URL` are required, the
rest have defaults.

## `ROTATION_SECRETS_URL`

Base URL of the secrets-manager REST API, for example
`https://secrets.internal.example.com`. The auditor appends `/v1/secrets` to
it when it lists the inventory. Required — the service refuses to start
without it.

## `ROTATION_STALE_AFTER`

How long a credential may go unrotated before the auditor flags it. A
credential that has gone unrotated for twice this long is escalated from
`DUE` to `OVERDUE`.

## `ROTATION_PAGE_SIZE`

How many credentials to ask the secrets manager for per listing request.
Defaults to `2000`. Production inventories run to roughly 40,000 credentials,
so lowering this mainly trades sweep latency for a smaller response body.

## `ROTATION_ALERT_RECIPIENTS`

Comma-separated list of addresses that receive the rotation digest, for
example `security@example.com,platform-oncall@example.com`. Defaults to
`security@example.com`.

## Runtime files

The container expects the internal CA trust store at
`/etc/rotation/truststore.p12` (see `ROTATION_TRUST_STORE` in the Dockerfile).
It is mounted by the deployment and is not part of the image build.
