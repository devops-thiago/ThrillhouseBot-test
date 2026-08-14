# queuewatch configuration

`queuewatch` is configured entirely through the process environment. It reads the
environment once at start-up (`src/config.zig`) and runs a single collection pass,
so a change only takes effect on the next run.

## Settings

### `QUEUEWATCH_METRICS_URL`

Base URL of the CI metrics API, without a trailing slash. A trailing slash is
stripped if you leave one on. Required — the process exits with
`MissingMetricsUrl` if it is unset.

    QUEUEWATCH_METRICS_URL=https://ci-metrics.internal.example.com

### `QUEUEWATCH_FLEET_ID`

Runner fleet whose queue waits are collected. Required — the process exits with
`MissingFleetId` if it is unset.

    QUEUEWATCH_FLEET_ID=fleet-4471

### `QUEUEWATCH_REGRESSION_RATIO`

How far above its baseline a pool has to queue before the run treats it as
regressed, as a ratio rather than a percentage: `0.5` means "half again as slow as
the baseline", `1.0` means "twice the baseline". Default: `0.5`.

    QUEUEWATCH_REGRESSION_RATIO=0.75

### `QUEUEWATCH_IGNORED_POOLS`

Pools that never take part in a report. Their samples are dropped before any
baseline is computed. Surrounding whitespace around an entry is ignored.
Default: empty, so every pool in the metrics window is reported on.

    QUEUEWATCH_IGNORED_POOLS=sandbox

## Output

Digests are written to the alert channel on stdout and staged as JSON under
`/var/spool/queuewatch/<pool>/<fleet>.json` for the fleet collector, which mounts
that directory. The path is fixed and is not configurable.
