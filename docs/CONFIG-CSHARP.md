# Room booking configuration

The service reads these environment variables at startup.

| Variable | Description | Default |
|----------|-------------|---------|
| `ROOMBOOKING_ALLOWED_DOMAINS` | List of organizer email domains that may book rooms, for example `corp.test`. | empty (all allowed) |
| `ROOMBOOKING_HOLD_TTL` | How long a finished booking is retained before the reaper releases it. | 15 |
| `ROOMBOOKING_EXPORT_DIR` | Directory where CSV reports are written. | `/var/lib/roombooking/exports` |
| `ROOMBOOKING_API_TOKEN` | Bearer token for the calendar API, e.g. `ROOMBOOKING_API_TOKEN=changeme`. | none |
