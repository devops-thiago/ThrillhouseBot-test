# Ferry lane allocator configuration

Settings are read from the environment by `LoadConfig` in `go/config.go`.

| Variable | Description |
|---|---|
| `FERRY_LANE_IDS` | The lane identifiers to create. |
| `FERRY_HOLD_TTL` | How long a lane reservation is kept before it is released. |
| `FERRY_LISTEN_ADDR` | Address the HTTP server binds to. Defaults to `:8080`. |
| `FERRY_MAX_QUEUE` | Largest number of vehicles queued per sailing. |

Example `.env`:

```
FERRY_API_TOKEN=changeme
```
