# Device reconciler configuration

All settings are read from the process environment at start-up by
`com.thrillhouse.devicerecon.ReconcilerConfig`. A value that cannot be parsed fails the start-up
rather than falling back to its default.

| Variable | Required | Description |
| --- | --- | --- |
| `RECON_MDM_BASE_URL` | yes | Base URL of the MDM's device API, without a trailing slash, e.g. `https://mdm.internal/api`. |
| `RECON_MDM_TOKEN` | yes | Bearer token for the MDM. Needs the device listing and device retirement scopes. |
| `RECON_DIRECTORY_BASE_URL` | yes | Base URL of the corporate directory's API. |
| `RECON_DIRECTORY_TOKEN` | yes | Bearer token for the directory's account lookup. Read-only. |
| `RECON_DB_URL` | no | JDBC URL of the local mirror. Default `jdbc:postgresql://localhost:5432/devices`. |
| `RECON_RETIREMENT_GRACE_DAYS` | no | Whole days after a departure date before the device may be retired. Default `30`. |
| `RECON_EXEMPT_PLATFORMS` | no | Platforms that are never retired automatically, matched case-insensitively. Default `shared-ipad,kiosk`. |
| `RECON_INTERVAL_MINUTES` | no | Minutes between scheduled runs. The first one runs an interval after start-up, not immediately. Default `360`. |
| `RECON_HTTP_PORT` | no | Listen port for the operator endpoints. Default `8080`. |
| `RECON_DRY_RUN` | no | When `true`, every decision is made and reported but nothing is retired. Default `false`. |

Run with `RECON_DRY_RUN=true` after changing the grace window or the exempt platforms: the report
lists exactly what the next real run would retire.

## What the policy does

The checks run in this order, and the first one that matches decides:

1. An enrolment the MDM already reports as retired is left alone.
2. An owner the directory has never heard of is escalated, never retired — contractor and partner
   addresses live outside the directory and their devices are legitimately enrolled.
3. An owner who is active or suspended keeps their devices. Suspension is a leave of absence or an
   open investigation, and neither is a reason to wipe a laptop.
4. A device on an exempt platform is escalated even when its owner has departed. Shared and kiosk
   devices are enrolled under whoever set them up, so the enrolling owner leaving says nothing about
   whether the device is still in service.
5. A departure with no date in the directory is escalated rather than dated by guesswork.
6. Anything left is retired once `RECON_RETIREMENT_GRACE_DAYS` have passed since the departure date.

## Operator endpoints

| Endpoint | Purpose |
| --- | --- |
| `GET /devices?owner=<email>` | The mirrored enrolments for one address, most recently checked in first |
| `POST /reconciliations` | Runs a reconciliation immediately and returns its report |
| `GET /reconciliations/latest` | The report of the last run, or `404` before the first one finishes |

## Tables

`enrolled_device` is the local mirror, keyed on the MDM's device id and refreshed on every run. It
keeps `retired_at` and `retire_reason` so a retirement stays answerable after the MDM has dropped
the device from its listing.
