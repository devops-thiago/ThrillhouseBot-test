# scanwatch configuration

scanwatch is configured entirely through environment variables, read at
startup in `main.go`.

## `SCANNER_API_URL`

Base URL of the image scanner service, e.g. `https://scanner.internal:8443`.
Required — scanwatch exits at startup if this is unset.

## `SCANNER_API_TOKEN`

Bearer token used to authenticate against the scanner API. No default;
omit it only if the scanner is configured to allow anonymous reads.

## `REPORT_DIR`

Directory where per-image scan reports are written as JSON files.
Defaults to `/var/lib/scanwatch/reports`.

## `POLL_INTERVAL`

How often scanwatch re-scans tracked images, as a Go duration string
(e.g. `5m`, `1h`). Defaults to `10m`.

## `ALERT_SEVERITIES`

Severities that should trigger an on-call alert when found, e.g. `critical`.
Defaults to `critical`.
