# cert-monitor configuration

cert-monitor is configured entirely through environment variables. All of
them are optional; sane defaults are used when a variable is unset.

## `CERT_MONITOR_INVENTORY_URL`

The base URL of the internal domain inventory API that cert-monitor pulls
its domain list from. Defaults to `https://inventory.internal/api/domains`.

## `CERT_MONITOR_EXTRA_DOMAINS`

Additional domains to check that aren't tracked in the inventory service,
useful for domains owned by this team but not yet registered upstream.

## `CERT_CHECK_TIMEOUT`

How long to wait for a TLS handshake before treating a domain as
unreachable. Defaults to `5`.

## `CERT_MONITOR_WARN_DAYS`

The number of days before expiry at which a certificate is flagged in the
digest. Defaults to `14`.
