# roomsvc configuration

`roomsvc` reads its settings once at start-up, from the environment. Nothing is
re-read while the process runs, so a change needs a restart.

## `ROOMSVC_LISTEN_ADDR`

Address the HTTP listener binds to, as `host:port`.

Default: `0.0.0.0:8080`.

## `ROOMSVC_FACILITIES_ENDPOINT`

`host:port` of the campus facilities export API. The booking feed is read from
`/api/v2/bookings` and the room inventory from `/api/v2/rooms` on that endpoint.

Default: `facilities.internal:80`.

## `ROOMSVC_SYNC_INTERVAL_SECS`

How long the sync thread waits between two passes over the facilities feed, in
seconds. A value that does not parse as a whole number falls back to the default.

Default: `300` (five minutes).

## `ROOMSVC_EXCLUDED_ROOMS`

Rooms that must be skipped when the facilities feed is mirrored. Use it for the
rooms of a tenant that runs its own booking system: their feed rows are dropped
before the double-booking scan runs, and the service never hands those rooms out.

A longer list can be mounted as a file instead and pointed at with
`ROOMSVC_EXCLUSIONS_FILE`; the two lists are merged.

## Calendar export

Rendered calendars are written to `/var/lib/roomsvc/exports` by the bundled
`ics-render` helper. Mount that path if the exports have to outlive the
container.
