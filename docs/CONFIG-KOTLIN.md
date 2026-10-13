# Desk invoicer configuration

The invoicer reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `BOOKING_API_TOKEN` | Bearer token for the booking platform API. Set it from your secret store. | sandbox token |
| `VAT_RATE_PERCENT` | VAT rate applied to each invoice subtotal, as a whole percentage. | `20` |
| `INVOICE_EXEMPT_MEMBERS` | Member IDs that are never invoiced (staff, partner organisations). | empty |
| `INVOICE_RUN_INTERVAL_MINUTES` | How often the invoicing run is triggered, in minutes. | `60` |

`BOOKING_API_URL`, `PAYMENT_API_URL`, `DATABASE_URL` and `HTTP_PORT` point the service at its
dependencies and default to local development values.

Example `.env` for local runs:

```
BOOKING_API_TOKEN=changeme
VAT_RATE_PERCENT=20
```
