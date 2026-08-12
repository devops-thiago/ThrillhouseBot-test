# Knowledge Search Widget Configuration

The search widget reads its configuration from Vite environment variables at build time.

| Variable | Description |
| --- | --- |
| `VITE_SEARCH_API_BASE_URL` | Base URL of the search API the widget queries. |
| `VITE_SEARCH_DEBOUNCE_MS` | How long to wait after the user stops typing before firing a search. |
| `VITE_SEARCH_PAGE_SIZE` | Number of results requested per page. |
| `VITE_ENABLED_CATEGORIES` | Categories shown as filter chips in the search bar. |

All variables are optional; sensible defaults are used when a variable is not set.
