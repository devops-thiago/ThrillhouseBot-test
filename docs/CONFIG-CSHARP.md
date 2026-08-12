# CertWatch configuration

CertWatch is configured entirely through environment variables. All variables
below are read once at startup in `Program.cs`.

## `CERTWATCH_CA_BASE_URL`

Base URL of the internal Certificate Authority inventory API that CertWatch
polls for certificate records (for example `https://ca-inventory.internal`).
Required — the process exits immediately if this is not set.

## `CERTWATCH_DB_CONNECTION_STRING`

Connection string for the asset-management database that holds the `owners`
table, used to resolve a certificate's owner id to a notification email
address. Required — the process exits immediately if this is not set.

## `CERTWATCH_EXPIRY_THRESHOLD_DAYS`

Number of days ahead of now to treat a certificate as "expiring soon" and
include it in the notification run. Default: `30`.

## `CERTWATCH_ALERT_EMAILS`

Operator addresses that receive a summary message whenever one or more
expiry notifications fail to send during a run. Default: none (no summary is
sent if this is unset).
