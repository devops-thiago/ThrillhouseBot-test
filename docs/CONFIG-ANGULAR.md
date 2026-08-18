# Configuration

The console reads its runtime settings from `window.__env`, which the `env.js`
file served next to the bundle populates. Nothing is baked into the build, so
the same artifact is promoted from staging to production unchanged. The keys are
parsed in `src/main.ts`.

| Variable | Description | Default |
| --- | --- | --- |
| `POSTMORTEM_API_BASE_URL` | Base URL of the incident API. | `https://incidents.internal.example.com/api` |
| `POSTMORTEM_PAGE_SIZE` | Postmortems requested per page of the review board. The API caps this at 200. | `40` |
| `POSTMORTEM_REFRESH_SECONDS` | How often the board reloads while it is open. | `120` |
| `POSTMORTEM_SEV1_SIGNOFFS` | Sign-offs a sev1 postmortem needs before it leaves the board. Everything below sev1 needs one. | `2` |
| `INCIDENT_TIMELINE_URL` | Base URL used to build incident timeline and tracker links. | `https://incidents.internal.example.com` |

An unset key falls back to the default above, so a deployment normally only sets
`POSTMORTEM_API_BASE_URL` and `INCIDENT_TIMELINE_URL`.

## env.js

```js
window.__env = {
  POSTMORTEM_API_BASE_URL: 'https://incidents.eu.example.com/api',
  POSTMORTEM_SEV1_SIGNOFFS: '2',
  INCIDENT_TIMELINE_URL: 'https://incidents.eu.example.com',
};
```

The image ships an empty `window.__env` so the bundle boots on its defaults; the
deployment mounts the file above over it.

## Reviewer identity

The signed-in reviewer is not configured. The console asks the API for it
(`GET /reviewers/me`) and uses the answer to decide whether the sign-off form is
still open on a given postmortem.
