# Configuration

`AgentFleetMonitor` is configured entirely through environment variables, read in
`src/AgentFleetMonitor/Program.cs`.

| Variable | Required | Description |
| --- | --- | --- |
| `AGENTFLEET_API_BASE_URL` | yes | Base URL of the fleet registry API that lists registered build agents (e.g. `https://fleet.internal.example.com`). |
| `AGENTFLEET_DB_CONNECTION_STRING` | yes | SQL Server connection string for the local agent cache. |
| `AGENTFLEET_WEBHOOK_URL` | yes | Chat-ops webhook URL that stale-agent alerts are posted to. |
| `AGENTFLEET_STALE_THRESHOLD` | no | How long an agent can go without a heartbeat before it is flagged as stale. |

## Notes

- The service polls the fleet registry once per run; schedule it with cron or a
  CI pipeline trigger for continuous monitoring.
- Alerts are only sent when at least one agent is flagged stale.
