# DLQ Reprocessor — configuration

The service is configured entirely through environment variables; there is no
config file to edit.

## `DLQ_QUEUE_URL`

Base URL of the dead-letter queue's HTTP API. Required — the service refuses
to start without it.

## `DLQ_PRIMARY_QUEUE_URL`

Base URL of the primary work queue that reprocessed messages are republished
to. Required.

## `DLQ_MAX_RETRIES`

Maximum number of reprocessing attempts before a message is left in the
dead-letter queue for manual review. Defaults to `3`.

## `DLQ_ALERT_EMAILS`

Email addresses to notify when a batch has messages that failed
reprocessing.
