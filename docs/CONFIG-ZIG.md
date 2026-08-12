# zigqueue configuration

zigqueue is configured entirely through environment variables. There is no
configuration file.

| Variable | Required | Default | Description |
|---|---|---|---|
| `QUEUE_COORDINATOR_URL` | yes | — | Base URL of the coordinator service zigqueue polls for pending job definitions. |
| `WORKER_POOL_SIZE` | no | `4` | Number of jobs drained from the queue and run per poll cycle. |
| `JOB_RETRY_BACKOFF` | no | `500` | Backoff delay applied between job retry attempts. |
| `QUEUE_DATA_DIR` | no | `./data` | Directory where job logs and queue state are written. Created on startup if missing. |
