# Expense approvals configuration

The dashboard reads these Vite environment variables in `src/config.ts`.

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Base URL of the expense API. Defaults to `http://localhost:8080`. |
| `VITE_PAGE_SIZE` | Number of reports requested per page. Defaults to 25. |
| `VITE_APPROVER_ROLES` | Roles allowed to approve or reject a report. |
| `VITE_POLL_INTERVAL` | How often the dashboard refreshes the pending list. |

The API token is supplied at build time:

    API_TOKEN=changeme
