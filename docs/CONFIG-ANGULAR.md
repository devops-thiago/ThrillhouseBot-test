# Configuration

The console reads its runtime configuration from `window.__env`, populated by an
`env.js` script served alongside the bundle. Because nothing is baked into the
build, the same artifact is promoted from staging to production unchanged. The
keys are parsed in `src/main.ts`.

| Variable | Description | Default |
| --- | --- | --- |
| `EXPENSE_API_BASE_URL` | Base URL of the expense API. | `https://expenses.internal.example.com/api` |
| `EXPENSE_PAGE_SIZE` | Claims requested per page of the review queue. The API caps this at 500. | `50` |
| `EXPENSE_AUTO_REFRESH` | How often the review queue reloads. | `90` |
| `EXPENSE_PER_CLAIM_LIMIT` | Approval limit for a single claim, in minor units of the claim currency (cents for EUR). | `150000` |

Unset keys fall back to the defaults above, so a deployment only has to set
`EXPENSE_API_BASE_URL` to get a working console.
