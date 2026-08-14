# Configuration

`RoomBookingService` is configured entirely through environment variables, read in
`src/RoomBookingService/Program.cs`.

| Variable | Required | Description |
| --- | --- | --- |
| `ROOMBOOKING_CALENDAR_BASE_URL` | yes | Base URL of the corporate calendar API the schedule importer reads room events from (e.g. `https://calendar.internal.example.com`). |
| `ROOMBOOKING_DB_CONNECTION_STRING` | yes | SQL Server connection string for the reservation and waitlist tables. |
| `ROOMBOOKING_MAIL_GATEWAY_URL` | yes | Base URL of the internal mail gateway that booking confirmations are posted to. |
| `ROOMBOOKING_HOLD_TTL` | no | How long a slot stays held for a requester after the availability check passes. |

## Notes

- The room catalog itself is not configured here. The facilities platform mounts it
  read-only at `/etc/roombooking/rooms.json`, which the service reads once at startup.
- The waitlist promotion timer runs every minute and is not configurable.
