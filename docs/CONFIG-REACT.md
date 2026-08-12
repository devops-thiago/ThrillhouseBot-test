# Configuration

The changelog viewer reads its configuration from Vite environment variables
at build time (`.env`, `.env.production`, etc.).

| Variable | Description | Default |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Base URL used for all release-notes API requests. | `/api` |
| `VITE_PAGE_SIZE` | Number of release notes requested per page. | `20` |
| `VITE_ENABLED_TAGS` | Which tags appear as filter chips above the release notes list. | (none) |
