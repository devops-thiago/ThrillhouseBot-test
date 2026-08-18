# staleguard configuration

`staleguard` is configured entirely through the process environment. The
environment is read once at start-up (`src/config.zig`) and one sweep runs per
invocation, so a change takes effect on the next run.

## Settings

### `STALEGUARD_CATALOG_URL`

Base URL of the backup catalog, without a trailing slash. A trailing slash is
stripped if you leave one on. Required — the process exits with
`MissingCatalogUrl` if it is unset.

    STALEGUARD_CATALOG_URL=https://backup-catalog.internal.example.com

### `STALEGUARD_ESTATE_ID`

Estate whose backup jobs are swept. Required — the process exits with
`MissingEstateId` if it is unset.

    STALEGUARD_ESTATE_ID=estate-eu-1

### `STALEGUARD_TIER_POLICY`

Per-tier freshness windows in hours, as a comma separated list of
`<tier>=<hours>` pairs. Whitespace around either side of a pair is ignored. A
tier listed twice keeps its first window and logs a warning. Default: empty, so
every dataset uses `STALEGUARD_DEFAULT_WINDOW_HOURS`.

    STALEGUARD_TIER_POLICY=gold=6,silver=24,archive=168

### `STALEGUARD_DEFAULT_WINDOW_HOURS`

Freshness window for a tier with no entry in `STALEGUARD_TIER_POLICY`. Must be
positive. Default: `24`.

    STALEGUARD_DEFAULT_WINDOW_HOURS=36

### `STALEGUARD_EXEMPT_DATASETS`

Datasets that never take part in a sweep. Their jobs are dropped before any
policy is applied, so they appear neither in the printed summary nor in the
status document. Default: empty.

    STALEGUARD_EXEMPT_DATASETS=ci-artifacts,scratch-vol

### `STALEGUARD_STATUS_PATH`

File the status document is written to. Parent directories are created if they
do not exist and the file is swapped in atomically, so a reader polling it on
its own schedule never sees a partial snapshot. Default:
`/var/lib/staleguard/status.json`.

    STALEGUARD_STATUS_PATH=/var/lib/staleguard/status.json

## Output

The summary is printed to stdout, worst dataset first. The same sweep is written
to the status path as a JSON snapshot:

    {
      "estate": "estate-eu-1",
      "generated_at": 1770000000,
      "datasets_checked": 42,
      "datasets_breaching": 2,
      "datasets": [
        {
          "dataset": "billing-ledger",
          "tier": "gold",
          "state": "stale",
          "age_hours": 9.0,
          "window_hours": 6,
          "failed_runs": 1
        }
      ]
    }

`state` is one of `fresh`, `stale` or `no_restore_point`, and `age_hours` is
`null` for a dataset with no restore point in the catalog's retention window.

## Exit codes

| Code | Meaning |
| --- | --- |
| `0` | Every dataset is inside its freshness window. |
| `2` | At least one dataset is stale or has no restore point. |
| `1` | The sweep itself failed, e.g. the catalog was unreachable. |

The check runner treats `2` as a warning it can page on and `1` as a broken
check, so the two are kept apart deliberately.
