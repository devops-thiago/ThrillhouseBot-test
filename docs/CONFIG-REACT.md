# Configuration

The macro library reads its configuration from Vite environment variables at
build time (`import.meta.env.*`). Set these in a `.env` file or in the CI/CD
environment before running `npm run build`.

## `VITE_API_BASE_URL`

Base URL of the macro service, e.g. `https://macros.internal.example.com/api`.
Required — the app will fail to load macros if this is unset. No default.

## `VITE_MACRO_PAGE_SIZE`

Number of macros requested per page from the list endpoint. Defaults to `20`
if unset or not a valid number.

## `VITE_ALLOWED_MACRO_TAGS`

Tags shown in the sidebar filter, restricting which macros an agent can
browse to. Defaults to an empty list (no restriction) if unset.

## `VITE_REQUEST_TIMEOUT_MS`

How long, in milliseconds, the app waits for a macro-service response before
giving up. Defaults to `5000` (5 seconds) if unset.
