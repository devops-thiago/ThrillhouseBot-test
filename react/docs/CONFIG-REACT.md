# Volunteer Shift Board: configuration

The board reads its settings from Vite environment variables at build time
(`import.meta.env`). Set them in `.env.local` for development, or pass the
matching build args (`SHIFT_API_URL`, `SHIFT_API_TOKEN`) to `docker build`.

| Variable | Default | Description |
|---|---|---|
| `VITE_SHIFT_API_URL` | `/api` | Base URL of the shifts API. |
| `VITE_SHIFT_ROLES` | _(empty: show all roles)_ | Roles to show on the board. |
| `VITE_BOARD_PAGE_SIZE` | `50` | Number of shifts requested per API page. |
| `VITE_SHIFT_API_TOKEN` | _(kiosk service account)_ | Bearer token sent to the shifts API. |

Example `.env.local` for a staging API:

```
VITE_SHIFT_API_URL=https://staging.example.org/api
VITE_SHIFT_API_TOKEN=<your-token>
```
