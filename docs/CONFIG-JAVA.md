# Room booking configuration

The service reads its settings from environment variables in `AppConfig.fromEnv`.

| Variable | Description | Default |
| --- | --- | --- |
| `ROOMBOOK_ALLOWED_DOMAINS` | E-mail domains whose organizers may book rooms. | `example.com` |
| `ROOMBOOK_HOLD_TIMEOUT` | How long an unconfirmed hold is kept before the sweeper releases it. | `15` |
| `ROOMBOOK_PAGE_SIZE` | Number of rooms requested from the directory per page. | |
| `ROOMBOOK_CALENDAR_TOKEN` | Token used for the calendar API. Example: `ROOMBOOK_CALENDAR_TOKEN=changeme` | |
