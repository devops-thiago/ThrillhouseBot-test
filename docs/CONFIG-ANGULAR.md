# Certificate watch configuration

The certificate watch feature reads its configuration from `window.__env`,
injected into `index.html` by the deploy pipeline at container start. This
lets one built image be reused across environments without an Angular CLI
rebuild.

Settings:

- `CERT_WATCH_API_BASE_URL` — base URL of the certificate scanner API the
  console calls.
- `CERT_WATCH_PAGE_SIZE` — number of certificates requested per page from
  the API. Default: 50.
- `CERT_WATCH_POLL_INTERVAL` — how often the console checks the scanner API
  for inventory changes.
- `CERT_WATCH_SEVERITY_FILTERS` — comma-separated list of severity levels
  shown by default in the console's filter dropdown. Default:
  `high,critical`.
