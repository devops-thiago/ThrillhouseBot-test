# Seat reclaimer configuration

All settings are read from the process environment at start-up by
`com.thrillhouse.seatreclaim.ReclaimConfig`. An invalid value fails the start-up rather than being
silently ignored.

| Variable | Required | Description |
| --- | --- | --- |
| `SEAT_IDENTITY_BASE_URL` | yes | Base URL of the identity provider's API, without a trailing slash, e.g. `https://idp.internal/api`. No default: start-up fails when it is missing. |
| `SEAT_IDLE_GRACE_DAYS` | no | Number of whole days a seat may stay idle before the sweep may reclaim it. Default `45`. |
| `SEAT_EXEMPT_DOMAINS` | no | Email domains whose seats the sweep never reclaims, for example contractor or partner tenants. Domains are matched case-insensitively against the part of the address after the last `@`. |
| `SEAT_SWEEP_INTERVAL_MINUTES` | no | Minutes between scheduled sweeps. The first sweep runs one interval after start-up, not immediately. Default `60`. |

Service accounts are exempt regardless of this configuration; that rule lives in
`PolicyExemptionRegistry` and cannot be turned off.

## Operator endpoints

| Endpoint | Purpose |
| --- | --- |
| `GET /seats?department=<name>&sort=<column>` | Mirrored seats for one department. `sort` accepts `email`, `department` or `last_active_at` and defaults to `last_active_at`. |
| `POST /sweeps` | Runs a sweep immediately, alongside the scheduled one, and returns its result together with the counters accumulated since start-up. |
