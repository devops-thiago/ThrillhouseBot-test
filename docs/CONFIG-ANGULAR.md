# Webhook dashboard configuration

The dashboard is a static Angular build served by nginx. Runtime settings are
injected by the container entrypoint into `window.__APP_CONFIG__` before the
app bootstraps, using the environment variables below.

| Variable | Description | Default |
| --- | --- | --- |
| `APP_API_BASE_URL` | Base URL the dashboard uses for all webhook API requests. | `/api` |
| `APP_DELIVERY_LOG_PAGE_SIZE` | Number of delivery attempts fetched per page in the delivery log view. | `50` |
| `APP_ALLOWED_EMBED_ORIGINS` | Origins permitted to embed the dashboard in an iframe. | *(none)* |
| `APP_REQUEST_TIMEOUT_MS` | Timeout, in milliseconds, applied to outgoing API requests before the dashboard shows a retry prompt. | `10000` |
