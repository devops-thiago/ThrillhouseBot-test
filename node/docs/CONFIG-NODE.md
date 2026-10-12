# grant-triage configuration

The service is configured entirely through environment variables.

| Variable | Required | Default | Description |
|---|---|---|---|
| `PORTAL_BASE_URL` | yes | none | Base URL of the grant portal API, e.g. `https://grants.example.org`. A trailing slash is stripped. |
| `TRIAGE_REVIEWERS` | no | empty | Reviewer handles to notify when applications need a human decision. When empty, no review requests are sent. |
| `TRIAGE_POLL_INTERVAL_SECONDS` | no | `900` | How often, in seconds, the scheduled triage run pulls new applications from the portal. |
| `TRIAGE_ARCHIVE_DIR` | no | `/var/lib/grant-triage/attachments` | Directory holding uploaded attachments, laid out as `<archive>/<application id>/<file name>`. |

## Credentials

`PORTAL_API_TOKEN` holds the bearer token for the grant portal. Set it in your
deployment's secret store; for local runs, put it in an untracked `.env` file:

```
PORTAL_API_TOKEN=<your-token>
```

## Endpoints

- `GET /healthz` returns `{"status":"ok"}`.
- `POST /triage/run` runs a triage pass immediately and returns the shortlist.
- `GET /applications/:id/attachments/:name` streams a stored attachment.
