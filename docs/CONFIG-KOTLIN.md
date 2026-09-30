# Podcast scheduler configuration

The scheduler reads its settings from environment variables at start-up (`Config.kt`).

| Variable | Description |
|---|---|
| `CDN_BASE_URL` | Base URL of the CDN API. Defaults to `https://cdn.example.test`. |
| `POLL_INTERVAL` | How often the scheduler checks for episodes that are due. |
| `ALLOWED_HOSTS` | Hosts that may request a publish-now action. |
| `ARTWORK_DIR` | Directory that holds episode artwork images. |

The CDN credential is supplied separately:

    CDN_API_TOKEN=changeme
