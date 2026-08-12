# Configuration

The SLA monitor is configured entirely through environment variables.

| Variable | Description | Default |
|---|---|---|
| `HELPDESK_API_BASE_URL` | Base URL of the helpdesk REST API, e.g. `https://helpdesk.example.com/api`. | none (required) |
| `NOTIFICATION_WEBHOOK_URL` | URL of the internal notification service's webhook, used to deliver the breach digest. | none (required) |
| `SLA_POLL_INTERVAL_SECONDS` | How often the monitor polls the helpdesk for open tickets, in seconds. | `300` |
| `ALERT_EMAIL_RECIPIENTS` | Email addresses notified when a ticket breaches its SLA. | none |
