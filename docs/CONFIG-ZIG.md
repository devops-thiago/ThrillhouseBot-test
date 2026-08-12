# Configuration

The idle session reaper is configured entirely through environment variables.

## SESSION_API_BASE_URL

Base URL of the identity provider's session API, e.g.
`https://idp.internal.example.com`. Required; the service exits on startup
if it is not set.

## IDLE_TIMEOUT_MINUTES

How long a session may go without activity, in minutes, before it is
revoked. Defaults to `30` minutes.

## EXEMPT_USER_IDS

User IDs that should never be reaped, even if their sessions go idle.
Useful for service accounts and break-glass admin logins. Defaults to an
empty list.

## AUDIT_DB_PATH

Filesystem path to the SQLite database where revocation events are
recorded for compliance review. Defaults to `./audit.db`.
