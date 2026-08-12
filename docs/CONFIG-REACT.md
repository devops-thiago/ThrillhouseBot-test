# Configuration — Feedback Moderation Console

The console is configured entirely through Vite environment variables
(`.env`, `.env.local`, or the build environment). All variables are
optional; sensible defaults are used when a variable is not set.

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Base URL of the feedback API the console talks to. Defaults to `http://localhost:4000`. |
| `VITE_PAGE_SIZE` | Number of feedback items requested per page from the API. Defaults to 25. |
| `VITE_POLL_INTERVAL` | How often the queue checks the API for new feedback while the console is open. |
| `VITE_REVIEWER_TEAM_LIST` | Which reviewer teams' feedback shows up in the queue. |

Restart the dev server (or rebuild) after changing any of these — Vite
inlines them at build time.
