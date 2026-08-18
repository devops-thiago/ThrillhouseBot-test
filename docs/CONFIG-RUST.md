# shiftdesk configuration

`shiftdesk` reads its settings once at start-up, from the environment. Nothing is
re-read while the process runs, so a change needs a restart.

## `SHIFTDESK_LISTEN_ADDR`

Address the HTTP listener binds to, as `host:port`.

Default: `0.0.0.0:8080`.

## `SHIFTDESK_ROSTER_ENDPOINT`

`host:port` of the staffing roster export. Rotas are read from `/api/v1/rotas`
and absences from `/api/v1/absences` on that endpoint. Both are read on every
sync pass; a pass that fails on either call leaves the previous mirror in place.

Default: `roster.internal:80`.

## `SHIFTDESK_SYNC_INTERVAL_SECS`

How long the sync thread waits between two passes over the roster export, in
seconds. A value that does not parse as a whole number falls back to the default.

Default: `600` (ten minutes).

## `SHIFTDESK_MUTED_ROTAS`

Rotas that are mirrored and served over the API but never reported on stderr.
Use it for the rotas a partner team owns: their gaps in cover and their
unacknowledged handovers are their problem, and our on-call channel should not
carry them.

A longer list can be mounted as a file instead and pointed at with
`SHIFTDESK_MUTED_ROTAS_FILE`; the two lists are merged.

## Handover digests

Rendered digests are written to `/var/lib/shiftdesk/digests` by the bundled
`handover-digest` helper. Mount that path if the digests have to outlive the
container.

## Times

Every time in the API is a whole number of minutes counted from the Unix epoch,
which is the form the roster export uses. Shift lengths are minutes too, so a
daily rota is `1440` and a twelve-hour rota is `720`.
