# Configuration

The dashboard reads its configuration from Vite environment variables at build time
(see `src/config.ts`). Set these in a `.env` file or in the build environment before
running `npm run build`.

## `VITE_API_BASE_URL`

Base URL the client uses for all activity and member API calls, e.g.
`https://api.example.com`.

## `VITE_ACTIVITY_PAGE_SIZE`

How many activity events to request per page from the backend.

## `VITE_ALLOWED_ORIGINS`

Origins that are permitted to embed the dashboard widget.

## `VITE_POLL_INTERVAL`

How often the dashboard checks for new activity in the background, once
polling is enabled.
