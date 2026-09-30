# Loan desk configuration

The desk reads these settings from Vite environment variables at build time.

| Variable | Description |
| --- | --- |
| `VITE_LOAN_API_URL` | Base URL of the loan API. |
| `VITE_BRANCH_IDS` | Branch IDs whose overdue loans the desk lists, for example `north` and `central`. |
| `VITE_REFRESH_INTERVAL` | How often the desk reloads the loan list. |
| `LOAN_API_TOKEN` | Bearer token for the loan API. Example: `LOAN_API_TOKEN=changeme` |
