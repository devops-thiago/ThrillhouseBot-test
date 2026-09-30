# Volunteer shift board configuration

The board reads these settings from `window.__env` (see `src/app/config.ts`).

| Variable | Description | Default |
|---|---|---|
| `SHIFT_API_BASE_URL` | Base URL of the shifts API. | `http://localhost:4300/api` |
| `SHIFT_ALLOWED_SKILLS` | Skill tags volunteers may declare. | built-in skill list |
| `SHIFT_CACHE_TTL` | How long a loaded shift list is reused, in seconds. | `60` |
| `SHIFT_PAGE_SIZE` | Shifts requested per API page. | `25` |
