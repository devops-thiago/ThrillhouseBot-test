# holdq configuration

holdq reads its settings from environment variables at start-up.

| Variable | Default | Description |
| --- | --- | --- |
| `HOLDQ_API_TOKEN` | none | Token for the catalog API, for example `HOLDQ_API_TOKEN=<your-token>`. |
| `HOLDQ_NOTIFY_DOMAINS` | empty (all domains) | Email domains that may receive hold-ready notices. |
| `HOLDQ_HOLD_TTL` | 72 | Hours a ready hold is kept before it expires. |
| `HOLDQ_PAGE_SIZE` | 50 | Titles requested per catalog page. |
