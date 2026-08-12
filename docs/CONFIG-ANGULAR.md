# Incident timeline configuration

The incident timeline feature reads its configuration from `window.__env`,
injected into `index.html` by the deploy pipeline at container start. This
lets one built image be reused across environments without an Angular CLI
rebuild.

Settings:

- `INCIDENT_API_BASE_URL` — base URL of the incident-events API the timeline
  calls.
- `INCIDENT_PAGE_SIZE` — number of events requested per page from the API.
  Default: 50.
- `INCIDENT_POLL_INTERVAL` — how often the timeline checks for new events
  while an incident is open.
- `INCIDENT_SEVERITY_LEVELS` — severity levels shown in the timeline's
  filter dropdown.
