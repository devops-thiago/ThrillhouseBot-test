# Beehive compactor configuration

The service reads these environment variables at startup (see `zig/src/config.zig`).

| Variable | Description | Default |
|---|---|---|
| `BEEHIVE_HIVE_IDS` | List of hive ids the service accepts readings for. | empty |
| `BEEHIVE_FLUSH_INTERVAL` | How often compacted buckets are flushed. | `300` |
| `BEEHIVE_DB_PATH` | Path of the SQLite database that receives buckets. | |
| `BEEHIVE_API_TOKEN` | Token for the readings API. | |

Example `.env`:

    BEEHIVE_API_TOKEN=changeme
