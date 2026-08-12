# Configuration

The license auditor is configured entirely through environment variables.

## `REGISTRY_BASE_URL`

Base URL of the internal package registry API that the auditor queries for
project package lists and audit logs. Defaults to
`https://packages.internal.example.com`.

## `REGISTRY_TIMEOUT_MS`

Per-request timeout, in milliseconds, applied to every registry API call.
Defaults to `5000`.

## `LICENSE_ALLOWLIST`

The set of licenses that are permitted for a package to pass the audit.
Comparison is case-insensitive. Defaults to `mit,apache-2.0,bsd-3-clause,isc`.

## `REPORT_DIR`

Directory that generated audit reports are archived into after each run.
Defaults to `reports`.
