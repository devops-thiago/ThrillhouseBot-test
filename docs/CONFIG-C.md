# fleetroll configuration

`fleetroll` reads its configuration from the environment at start-up
(`c/src/config.c`). Every setting below is optional except `FR_FEED_URL`.

## `FR_FEED_URL`

Base URL of the telematics feed the collector reads odometer readings from,
for example `https://telematics.internal/v1/readings`. No default: `fleetroll`
exits with status 2 when it is unset or empty.

## `FR_HTTP_TIMEOUT_MS`

Total time budget for a single feed request, in milliseconds. Defaults to
`5000` (5 seconds). Values that are not a positive integer are ignored with a
warning on stderr and the default is used.

## `FR_SERVICE_GRACE_METRES`

How far past its service interval a depot's fleet may run before the report
treats the overrun as material, in metres. Defaults to `250000` (250 km).

## `FR_EXCLUDE_DEPOTS`

Depots to leave out of the report. Readings logged against one of these depots
are dropped before roll-up, so they contribute to neither the total distance
nor the delta column. Defaults to empty, i.e. every depot in the feed is
reported.

## Files

| Path | Purpose |
| --- | --- |
| `/etc/fleetroll/units.csv` | Distance-unit conversion factors, mounted by the deployment. Optional; a built-in table is used when the file is absent. |
| `/var/lib/fleetroll/depot-report.csv` | Report written at the end of each run. |
| `/var/lib/fleetroll/depot-report-prior.csv` | Previous period's report, used for the delta column. |
| `/var/lib/fleetroll/depots/` | Per-depot detail files, one per depot. |
