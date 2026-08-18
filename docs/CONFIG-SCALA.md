# certguard configuration

`certguard` reads its settings from the environment once, in `Settings.fromEnv`,
when the process starts. A missing required variable stops the process before
the status server binds.

## Required

| Variable | Meaning |
|---|---|
| `CERTGUARD_REGISTRY_URL` | Base URL of the service registry. The sweep reads `GET /v2/hosts?tls=true`, following the `next` token until the listing is exhausted. |
| `CERTGUARD_CA_URL` | Base URL of the internal CA. The sweep reads `GET /v1/issued`. |
| `CERTGUARD_RELAY_URL` | Notification relay the per-owner digests are posted to. |
| `CERTGUARD_JDBC_URL` | JDBC URL of the Postgres database holding `certificate_finding` and `certificate_suppression`. |

## Optional

| Variable | Default | Meaning |
|---|---|---|
| `CERTGUARD_RENEW_WITHIN_DAYS` | `30` | Remaining lifetime at which a host enters the renewal window and is reported as `renewable`. |
| `CERTGUARD_URGENT_WITHIN_DAYS` | `7` | Remaining lifetime at which a host is reported as `urgent` instead. Must not exceed the renewal window; the process refuses to start if it does. |
| `CERTGUARD_SWEEP_MINUTES` | `30` | Delay between the end of one sweep and the start of the next. |
| `CERTGUARD_IGNORED_OWNERS` | empty | Comma-separated owners whose hosts are skipped entirely, matched case-insensitively against the registry `owner` field. |
| `CERTGUARD_STATUS_PORT` | `9310` | Port the status server binds. |
| `CERTGUARD_API_TOKEN` | empty | Bearer token sent to the registry, the CA and the relay. Omitting it sends no `Authorization` header, which is what the local stub setup does. |
| `CERTGUARD_JDBC_USER` | `certguard` | Database user. |
| `CERTGUARD_JDBC_PASSWORD` | empty | Database password; the deployment injects it from the `certguard-db` secret. |

Values that are not whole positive numbers fall back to the default rather than
failing the start-up, so a typo in the renewal window cannot take the sweep out
of service.

## Status surface

| Path | Answer |
|---|---|
| `GET /healthz` | `{"status":"ok"}` as soon as the process is up. |
| `GET /status` | Totals of the last completed sweep, or `{"state":"pending"}` before the first one finishes. |

## Suppressions

A row in `certificate_suppression` (`host`, `suppressed_until`, `reason`) keeps
a host out of the sweep until the timestamp passes. Rows are added by hand
while a host is mid-migration; nothing in certguard writes to that table.
