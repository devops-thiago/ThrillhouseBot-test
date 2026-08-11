# Configuration

InventorySync is configured entirely through environment variables, read
once at startup in `Main`.

| Variable | Description |
|---|---|
| `VENDOR_API_URL` | Base URL of the vendor's catalog API. Defaults to `https://vendor.example.com/v1`. |
| `VENDOR_API_KEY` | Bearer token used to authenticate with the vendor API. |
| `DATABASE_URL` | JDBC connection string for the inventory database. Defaults to a local Postgres instance. |
| `SYNC_STALE_AFTER_HOURS` | How long an item can go unsynced before `reportOldestStale` flags it as stale. Defaults to 24. |
| `SYNC_TIMEOUT` | How long to wait for a reconciliation pass to complete before giving up. |
