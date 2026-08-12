# Configuration

The dispatcher is configured entirely through environment variables.

## `WEBHOOK_TENANT_ID`

The tenant this dispatcher instance serves. Defaults to `default`.

## `WEBHOOK_ALLOWED_SUBSCRIBER_IDS`

Subscriber ids that are allowed to receive events. If unset, no subscribers are
allow-listed.

## `WEBHOOK_RETRY_BACKOFF_MAX`

The maximum backoff delay, in seconds, between retry attempts. Defaults to `30`.

## `WEBHOOK_METRICS_QUEUE_CAPACITY`

The number of delivery attempts the in-memory metrics queue can hold before
`MetricsRecorder.record` starts returning `false`. Defaults to `1000`.
