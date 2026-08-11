# Configuration

The renewal service reads its configuration from environment variables (or
the equivalent `appsettings.json` keys) at startup.

| Variable | Description |
| --- | --- |
| `ConnectionStrings__Billing` | SQL Server connection string for the billing database. Required. |
| `RENEWAL_REMINDER_WINDOW_DAYS` | Number of days before a subscription's expiry to start sending renewal reminders. Default: `7`. |
| `RENEWAL_CHECK_INTERVAL` | How often the background worker checks for subscriptions that need a reminder. |
| `RENEWAL_OPT_OUT_EMAILS` | Customers who should never receive a renewal reminder email. |
| `BILLING_API_BASE_URL` | Base URL of the billing provider's REST API. Default: `https://billing.internal/`. |
