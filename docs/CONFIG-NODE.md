# Configuration

`spend-allocator` is configured entirely through environment variables, all of which are read once at
start-up by `src/config.js`.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `BILLING_API_URL` | yes | — | Base URL of the billing provider's REST API, used for the cost and inventory endpoints. |
| `WAREHOUSE_URL` | yes | — | Endpoint of the warehouse's SQL-over-HTTP interface, used by the per-team report. |
| `ALLOCATION_TAG_KEYS` | no | `team` | Resource tag keys used to attribute spend to a team. The first key a resource carries wins, so list the most specific key first. |
| `BUDGET_FILE` | no | `/etc/spend-allocator/budgets.json` | Path to the monthly team budgets file. The deployment mounts this file; it is not part of the image. |

The budgets file is a JSON object mapping a team name to its monthly limit in whole currency units:

    {"platform": 4500, "data-eng": 1200}

A team that is absent from the file has no budget and is never flagged as over budget. A team whose
limit is `0` is flagged as soon as it books any spend at all.
