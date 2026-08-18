# caldrift configuration

`caldrift` reads its configuration from the environment at start-up
(`c/src/config.c`). Every setting is optional; the defaults are the ones the
calibration team already works to.

## `CD_ARCHIVE_DIR`

Root of the calibration archive. `caldrift` walks it recursively and reads every
`*.csv` file it finds, so the usual layout of one directory per collection round
works without any extra configuration. Rows are
`sensor_id,site_id,unit,reference,measured,days_since_service`; blank lines,
`#` comments and the column header the newer field kits write are ignored, and a
row that does not parse is reported on stderr and skipped.

Default: `/var/lib/caldrift/records`.

## `CD_TOLERANCE_PPM`

How far a sensor may deviate from its reference before it counts as drifted, in
parts per million of the reference. Drift is a ratio, so this is the same number
whatever unit the record was logged in.

Default: `2000` (0.2%). Values that are not a positive integer are ignored with a
warning on stderr and the default is used.

## `CD_SERVICE_INTERVAL_DAYS`

How long a sensor may go between services before the report raises it. This is
compared against the `days_since_service` column of the record, which the field
kit fills in from the sensor's own service tag.

Default: `365`.

## `CD_EXCLUDE_SITES`

Sites to leave out of the report, comma separated. Records logged against one of
these sites are dropped before roll-up, so they appear in neither the site rows
nor the skipped-record count. Use it for the sites a contractor calibrates on
their own schedule.

Default: empty, i.e. every site in the archive is reported.

## Units

A record whose unit is not in the conversion table is not comparable — an
unrecognised unit is as likely to be a misconfigured field kit as a new unit — so
it is skipped and counted in `# skipped_records` rather than rolled up at the
wrong scale. The built-in table covers `Pa`, `kPa`, `bar`, `psi`, `V`, `mV`,
`lpm`, `m3h` and `degC`, and can be extended or corrected from the deployment's
`units.csv`, one `unit,scale` row per line, where `scale` is the factor against
the base unit of the quantity. Only ratio units belong there: a unit with an
offset (`degF`) cannot be expressed as a single factor, which is why the field
kits are configured to log temperature in `degC`.

## Files

| Path | Purpose |
| --- | --- |
| `/etc/caldrift/units.csv` | Unit conversion factors, mounted by the deployment. Optional; the built-in table is used when the file is absent. The path is the first argument to the binary. |
| `/var/lib/caldrift/records/` | Calibration archive, walked on every run. |
| `/var/lib/caldrift/drift-report.csv` | Report written at the end of a run, staged as `.tmp` and renamed into place. |
| `/var/lib/caldrift/sites/` | Per-site detail files, one per site. |
