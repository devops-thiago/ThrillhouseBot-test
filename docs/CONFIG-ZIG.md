# printq configuration

printq reads its settings from environment variables at startup.

| Variable | Description | Default |
|---|---|---|
| `PRINTQ_ALLOWED_PRINTERS` | Printer names jobs may be sent to. | `front-desk` |
| `PRINTQ_POLL_INTERVAL` | Seconds to wait between queue polls. | `5` |
| `PRINTQ_MAX_COPIES` | Maximum number of copies a single job may request. | `10` |
| `PRINTQ_API_TOKEN` | Credential for the print backend. | none |

Example:

    PRINTQ_API_TOKEN=changeme
    PRINTQ_POLL_INTERVAL=10
