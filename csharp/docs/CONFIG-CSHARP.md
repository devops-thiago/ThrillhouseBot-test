# Holds service configuration

The holds service reads its settings from environment variables at startup
(`HoldsService/HoldsOptions.cs`).

| Variable | Default | Description |
|---|---|---|
| `HOLDS_BRANCH` | `central` | Branch code whose shelf the expiry worker checks against the catalog. |
| `HOLDS_SLIP_ROOT` | `/var/lib/holds/slips` | Directory holding printed hold slips, one sub-folder per branch. |
| `HOLDS_PICKUP_WINDOW` | `72` | How long a patron has to collect a ready hold before it expires and the next patron is offered the copy. |
| `HOLDS_CATALOG_TOKEN` | none | Bearer token sent to the branch catalog API. |

Example `.env` for local runs:

```
HOLDS_BRANCH=central
HOLDS_SLIP_ROOT=./slips
HOLDS_PICKUP_WINDOW=48
HOLDS_CATALOG_TOKEN=changeme
```
