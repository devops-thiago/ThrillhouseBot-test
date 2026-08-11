# Team directory configuration

The team directory feature reads its configuration from `window.__env`, which is
injected into `index.html` at container start (see `Dockerfile`). This lets one
built image be reused across environments without an Angular CLI rebuild.

Settings:

- `API_BASE_URL` — base URL of the team-members API the directory calls.
- `TEAM_PAGE_SIZE` — number of members requested per page from the API.
- `ALLOWED_DEPARTMENT_IDS` — department ids the directory is allowed to display.
- `CACHE_TTL` — how long a fetched roster is considered fresh before the
  directory refetches it.
