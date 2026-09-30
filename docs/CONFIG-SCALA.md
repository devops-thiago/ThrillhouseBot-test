# Room booking configuration

The service reads these environment variables in `scala/src/main/scala/rooms/Config.scala`.

| Variable | Description | Default |
|---|---|---|
| `ROOMS_PORT` | HTTP port to listen on | `8080` |
| `ROOMS_ALLOWED_TAGS` | List of room tags that may be requested | |
| `ROOMS_HOLD_TIMEOUT` | How long an unconfirmed hold is kept before it is released | |
| `ROOMS_DATA_FILE` | Path of the bookings CSV file | `/data/bookings.csv` |

Directory API credentials are passed with `API_TOKEN`:

    API_TOKEN=changeme
