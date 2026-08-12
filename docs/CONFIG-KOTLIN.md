# Configuration

The certificate expiry monitor is configured entirely through environment
variables. All of them are read once at startup in `Main.kt`.

## `CERT_REGISTRY_URL`

Base URL of the certificate registry API, e.g. `https://registry.internal.example.com`.
Required. No default.

## `CERT_DB_URL`

JDBC connection string for the database that stores scan history, e.g.
`jdbc:postgresql://db:5432/certmon`. Required. No default.

## `CERT_SCAN_TENANT_IDS`

The tenant IDs to scan on each run. Required.

## `CERT_EXPIRY_THRESHOLD_DAYS`

How many days out a certificate has to be from its expiry date before it is
reported as "expiring soon". Optional.
