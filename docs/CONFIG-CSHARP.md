# Configuration — Invoice Exporter (C#)

The invoice exporter reads its configuration from environment variables at startup, via
`ExportOptions.FromEnvironment()`. There is no config file.

## Settings

### `EXPORT_DB_CONNECTION_STRING`

Connection string for the invoices database. Required — the service throws on startup if
this is not set.

### `EXPORT_ACCOUNTING_API_URL`

Base URL of the internal Accounting API used to look up customer names and emails for each
exported invoice. Defaults to `https://accounting.internal/` when unset.

### `EXPORT_TENANT_FILTER`

Restricts the export to a single tenant's invoices. Leave unset to export invoices across all
tenants.

### `EXPORT_OUTPUT_DIRECTORY`

Directory the CSV export files are written to. Created automatically on startup if it does
not already exist. Defaults to `./export-output`.

### `EXPORT_WEBHOOK_URL`

Webhook endpoint notified with a summary each time an export run finishes. Leave unset to
disable the notification.

### `EXPORT_POLL_INTERVAL`

How often the worker checks the database for pending invoices.

### `EXPORT_ALLOWED_STATUSES`

Invoice statuses to include in the export.
