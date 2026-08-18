# Postmortem sign-off console — configuration

Everything is read at build time from Vite environment variables, so the values
have to be in the shell (or in a `.env` file) when `npm run build` runs. They are
parsed once in `src/config.js`.

## `VITE_INCIDENT_API_URL`

Base URL of the incident API the console talks to. Trailing slashes are
stripped. Defaults to `http://localhost:8080`.

## `VITE_REVIEW_PAGE_SIZE`

How many postmortems the console asks for when it loads the review queue. The
API caps this at 200. Defaults to `40`.

## `VITE_SEV1_REVIEWERS`

Reviewers a sev1 postmortem needs before it leaves the queue. Everything below
sev1 needs one. Defaults to `2`.

## `VITE_REVIEW_POLL_SECONDS`

How often the queue is reloaded while the console is open, so postmortems other
people have signed off disappear without a page reload. Defaults to `120`.

## Reviewer identity

The signed-in reviewer is not configured. The console asks the API
(`GET /reviewers/me`) and uses the answer to decide whether the sign-off form is
still open on a postmortem this reviewer has already been through.
