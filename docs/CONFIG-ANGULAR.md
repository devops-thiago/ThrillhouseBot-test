# Configuration

The dashboard reads its runtime configuration from `window.__env` (populated
by an `env.js` script loaded before the Angular bundle), so the same build
can be deployed to any environment without a rebuild. Values are parsed in
`src/main.ts`.

| Variable | Description | Default |
| --- | --- | --- |
| `ONCALL_API_BASE_URL` | Base URL of the on-call scheduling API. | `https://oncall.internal.example.com/api` |
| `ONCALL_PAGE_SIZE` | Number of shifts requested per page when loading the schedule. | `25` |
| `ONCALL_POLL_INTERVAL_MS` | How often the dashboard refreshes the schedule, in milliseconds. | `60000` |
| `ONCALL_ESCALATION_EMAILS` | Contact emails notified when a shift has no acknowledged coverage. | _(none)_ |
