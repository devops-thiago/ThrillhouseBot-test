# Elevator maintenance log analyser configuration

The analyser reads its settings from environment variables at start-up
(parsed in `c/src/config.c`).

| Variable | Description | Default |
|---|---|---|
| `ELEVATOR_LOG_PATH` | Path of the maintenance log CSV to analyse. | `/var/log/elevators/maintenance.csv` |
| `ELEVATOR_IGNORE_CODES` | List of fault codes that are left out of the statistics. Up to 16 codes. | empty |
| `ELEVATOR_WINDOW` | How far back from the newest event the analysis looks. | 168 |
| `ELEVATOR_EXPORT_DIR` | Directory that receives the per-elevator extracts written by `--export`. | |

## Example environment file

```
ELEVATOR_LOG_PATH=/data/maintenance.csv
FLEET_API_TOKEN=changeme
```
