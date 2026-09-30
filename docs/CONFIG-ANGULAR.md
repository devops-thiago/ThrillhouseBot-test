# Parcel tracking board configuration

The board reads these settings through `parseConfig` in `angular/src/app/config.ts`.

| Variable | Description |
| --- | --- |
| `TRACKING_API_URL` | Base URL of the tracking API. |
| `TRACKING_API_TOKEN` | Bearer token used for API calls. Example: `TRACKING_API_TOKEN=<your-token>`. |
| `CARRIER_ALLOWLIST` | List of carrier codes shown on the board, for example `ups`. |
| `SHIPMENT_POLL_INTERVAL` | How often the board polls the tracking API. Default `30000`. |
