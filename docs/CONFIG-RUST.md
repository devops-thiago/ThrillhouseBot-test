# Cold-chain logger configuration

The logger reads its settings from environment variables (parsed in `rust/src/config.rs`).

| Variable | Description | Default |
|---|---|---|
| `COLDCHAIN_SENSOR_IDS` | List of sensor ids the poller should watch, for example `fridge-1,fridge-2`. | empty |
| `COLDCHAIN_POLL_INTERVAL` | How often the poller asks the remote API for the latest reading. | 30 |
| `COLDCHAIN_LOG_DIR` | Directory holding one `<sensor>.log` file per sensor. | `/var/log/coldchain` |
| `COLDCHAIN_MAX_BATCH` | Maximum number of readings accepted in one batch. | |

Example `.env`:

    COLDCHAIN_API_TOKEN=changeme
