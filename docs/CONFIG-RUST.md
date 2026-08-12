# Configuration

erasure-processor is configured entirely through environment variables. All
of them are optional; sensible defaults are used when unset.

## `ERASURE_API_BASE_URL`

Base URL of the privacy platform's erasure-request API, used to list
pending requests and to submit deletions.

- Default: `http://localhost:8081`

## `SUBMISSION_GRACE_SECS`

How long a request may sit overdue past its fulfillment deadline, in
seconds, before the processor submits the deletion.

- Default: `86400`

## `POLL_INTERVAL_SECS`

How often, in seconds, the processor polls the privacy platform for the
current request backlog and runs a processing pass.

- Default: `300`

## `EXEMPT_ACCOUNT_IDS`

Account ids that are exempt from automatic erasure, such as accounts under
an active legal hold.
