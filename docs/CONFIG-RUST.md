# Configuration

hookrelay is configured entirely through environment variables. All of them
are optional; sensible defaults are used when unset.

## `HOOKRELAY_DB_PATH`

Filesystem path to the SQLite database file used to store registered
subscribers and the delivery log.

- Default: `hookrelay.db` (created in the working directory if it doesn't
  exist)

## `HOOKRELAY_SUBSCRIBER_API_URL`

Base URL of the upstream subscriber directory service. Used by
`/subscribers/sync` to pull the current list of subscribers for a topic.

- Default: `http://localhost:9000`

## `HOOKRELAY_PORT`

TCP port the HTTP server binds to on `0.0.0.0`.

- Default: `8080`

## `HOOKRELAY_ALLOWED_TOPICS`

Restricts event ingestion to an allow-list of topics. Requests to `/events`
for a topic outside this list are rejected with `403 Forbidden`. Leave unset
to accept events for any topic.
