# Asset loan dashboard configuration

The dashboard is a static bundle, so settings are read at build time from
`VITE_*` environment variables (see `src/config.ts`).

| Variable | Description | Default |
|---|---|---|
| `VITE_ASSET_API_URL` | Base URL of the inventory/loans API. | `/api` |
| `VITE_LOAN_SITES` | Sites shown in the dashboard; the first one is opened when no `?site=` query parameter is given. | none (falls back to `HQ`) |
| `VITE_PAGE_SIZE` | Loans shown per table page. Must be a positive integer. | `25` |
| `VITE_GRACE_DAYS` | Days after the due date before a loan counts as overdue. | `2` |

## API token

The bundle authenticates to the inventory API with a bearer token. Pass it as a
build argument; for local development put it in `.env.local`:

```
VITE_ASSET_API_TOKEN=<your-token>
```

```
docker build --build-arg API_TOKEN=<your-token> -t asset-loans react/
```
