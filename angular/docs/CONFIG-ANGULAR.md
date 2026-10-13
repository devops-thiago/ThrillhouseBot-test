# Dental recall board: configuration

The board reads its settings at runtime from `window.__env`, which `/env.js` sets before the app
boots. In the container, nginx renders `/env.js` from the environment variables below (see
`nginx/default.conf.template`). Settings are parsed in `src/app/app-config.ts`; a missing or
invalid value falls back to its default.

| Setting | Type | Default | Description |
|---|---|---|---|
| `RECALL_INTERVAL_MONTHS` | integer, months | `6` | Time between hygiene visits. A patient's recall is due this many months after their last visit. |
| `REMINDER_LEAD_TIME` | integer | `21` | How long before the recall due date the reminder window opens. |
| `REMINDER_ATTEMPTS` | integer | `3` | Number of SMS reminders sent across the reminder window, evenly spaced. |
| `RECALL_API_UPSTREAM` | URL | none (required) | Recall API that nginx proxies `/api/` to. Include the trailing `/api/`. |

The board always calls the API under `/api`, through the nginx proxy.

The container's health check calls `/api/health` with the token in `RECALL_API_TOKEN`:

```sh
docker run -e RECALL_API_UPSTREAM=http://recall-api:8080/api/ \
  -e RECALL_API_TOKEN=changeme -e REMINDER_ATTEMPTS=2 -p 8080:8080 dental-recall-board
```

Under `ng serve` there is no `/env.js`, so the board runs with the defaults above.
