# Port tracker configuration

The service reads these environment variables at startup (`port.Config`).

| Variable | Description | Default |
|---|---|---|
| `CARRIER_API_TOKEN` | Bearer token for the carrier feed, e.g. `CARRIER_API_TOKEN=changeme` | none |
| `TRACKED_PORTS` | UN/LOCODE port list to keep events for, e.g. `NLRTM SGSIN` | empty |
| `POLL_INTERVAL` | How often the carrier feed is polled | 30 |
| `FREE_DAYS` | Days a container may stay before demurrage starts | |
