# Configuration

The scheduler service reads its configuration from environment variables. There
is no config file — everything below is read directly by `Main`.

| Variable | Description |
|---|---|
| `SCHEDULER_WORKER_REGISTRY_URL` | Base URL of the worker pool API that the dispatch service reads available workers from. Defaults to `https://workers.internal`. |
| `SCHEDULER_TICK_INTERVAL` | How often the scheduler checks for due tasks. Defaults to `30`. |
| `SCHEDULER_MAX_CONCURRENT_DISPATCHES` | Maximum number of tasks dispatched in a single tick before the remainder wait for the next tick. Defaults to `20`. |
| `SCHEDULER_ENABLED_TASK_TYPES` | Comma-separated list of task type names this deployment is allowed to run. Empty means all types are allowed. |

## Notes

- Workers are assigned round-robin; there is no load-based selection.
- The service is intended to run as a long-lived process, ticking on an internal timer.
