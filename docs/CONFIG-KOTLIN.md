# Configuration

The order service reads its configuration entirely from environment
variables. There is no config file.

## Settings

- **`DB_URL`** — JDBC connection string for the orders database
  (for example `jdbc:postgresql://db:5432/orders`). Required; the service
  will not start without it.

- **`INVENTORY_BASE_URL`** — Base URL of the warehouse inventory service
  that the order service calls to check stock levels. Defaults to
  `http://inventory.internal:9000`.

- **`AUDIT_LOG_TOPICS`** — Restricts audit logging to these event names.
  When unset, every event is recorded.

- **`INVENTORY_REQUEST_TIMEOUT_MS`** — Timeout, in milliseconds, for
  outbound HTTP calls to the inventory service. Defaults to `5000`.
