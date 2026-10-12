# pickbatch configuration (Go)

The pick-list batching service in `go/` reads its settings from environment
variables at startup.

| Variable | Description | Default |
|---|---|---|
| `PICKBATCH_WMS_URL` | Base URL of the warehouse management system API. | `http://wms.internal:8080` |
| `PICKBATCH_ZONES` | Pick zones the scheduler batches automatically. | `A` |
| `PICKBATCH_INTERVAL` | How often the scheduler builds new batches. | `30` |
| `PICKBATCH_MAX_ORDERS` | Maximum number of orders in one pick batch. | |

The WMS integration token is read from `PICKBATCH_API_TOKEN`. For local runs,
put it in your environment file:

    PICKBATCH_WMS_URL=http://localhost:9000
    PICKBATCH_API_TOKEN=changeme

Slot locations are read from `/etc/pickbatch/slots.json`, which the slotting
tool exports; mount it into the container.
