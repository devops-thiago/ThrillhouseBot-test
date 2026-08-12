# Configuration

vulnscan-aggregator is configured entirely through environment variables.

## SCANNER_API_BASE_URL

Base URL of the vulnerability scanner's REST API (for example
`https://scanner.internal.example.com`). Required, no default.

## DB_URL

JDBC connection string for the findings database. Required, no default.

## ALERT_WEBHOOK_URL

Webhook endpoint the quarantine service posts to when an image crosses the
critical-finding threshold. Required, no default.

## CRITICAL_THRESHOLD

Minimum severity weight (see `Severity`) that triggers quarantine for an
image. Default: `4`.

## TRACKED_IMAGES

Container images to scan for vulnerabilities on each run, matched by name.
Required, no default.
