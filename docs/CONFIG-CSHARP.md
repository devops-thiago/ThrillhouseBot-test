# Configuration

`ShiftRelay` is configured entirely through environment variables, read once in
`src/ShiftRelay/Program.cs`. The three required ones have no defaults: the service
refuses to start without them rather than coming up half-wired.

| Variable | Required | Description |
| --- | --- | --- |
| `SHIFTRELAY_WORKFORCE_BASE_URL` | yes | Base URL of the workforce API the agreed shift swaps are imported from (e.g. `https://workforce.internal.example.com`). |
| `SHIFTRELAY_DB_CONNECTION_STRING` | yes | SQL Server connection string for the `Handovers` and `ShiftOverrides` tables. |
| `SHIFTRELAY_CHAT_WEBHOOK_URL` | yes | Webhook the on-call channel messages are posted to. |
| `SHIFTRELAY_ACK_TIMEOUT_MINUTES` | no | How long the incoming engineer has to acknowledge a handover before it is escalated. Defaults to `20`. |

## Notes

- The rota catalog is not configured here. The on-call platform mounts it read-only
  at `/etc/shiftrelay/rotas.json` and the service reads it once at startup, so a new
  rota needs a restart (or a rolling deploy, which is what the platform does anyway).
- The escalation sweep runs every five minutes and is not configurable. It only ever
  looks at handovers that are still `Pending`.
- The schedule endpoint defaults to a thirty-day horizon; callers can shorten or
  lengthen it per request with `?days=`.
