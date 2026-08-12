# Webhook Relay configuration

The service reads its configuration from environment variables. All of the settings below can
also be set via `appsettings.json` using ASP.NET Core's standard configuration precedence.

## QUEUE_BASE_URL

Base URL of the internal event queue that the relay polls for pending deliveries.

- Type: string (absolute URL)
- Default: `http://localhost:6100`

## ALLOWED_EVENT_TYPES

Restricts which event types the relay will accept when subscriptions are queried through
`/subscriptions`. Requests for any other event type are rejected with `400 Bad Request`.

- Type: string
- Default: `order.created,order.cancelled`

## RETRY_DELAY_SECONDS

How long the relay waits, in seconds, between delivery attempts to the same subscriber.

- Type: integer (seconds)
- Default: `2`

## WEBHOOK_SEND_TIMEOUT_MS

How long the relay waits, in milliseconds, for a subscriber to respond before treating the
delivery attempt as failed.

- Type: integer (milliseconds)
- Default: `5000`
