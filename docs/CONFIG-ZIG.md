# Configuration

certmon reads its configuration from environment variables at startup.

## `CERTMON_INVENTORY_URL`

Base URL of the internal domain inventory service that certmon queries for
the list of domains to check. Defaults to `http://inventory.internal/api/domains`
if unset.

## `CERTMON_CHECK_TIMEOUT`

Timeout applied to each per-domain openssl handshake, in seconds. Defaults
to `5`.

## `CERTMON_WARN_DAYS`

Number of days before expiry at which a certificate is flagged in the
report as expiring soon. Defaults to `14`.

## `CERTMON_ALLOWED_DOMAINS`

Restricts certificate checks to a specific set of domains, overriding the
full inventory listing. Unset means all domains returned by the inventory
service are checked.
