# Lift pass validator configuration

The service reads these environment variables at start-up (see `Settings.cs`).

| Variable | Description | Default |
|---|---|---|
| `LIFTPASS_DB_PATH` | Path of the SQLite file holding the pass table. | `lifts.db` |
| `LIFTPASS_ALLOWED_ZONES` | Zones this validator will admit passes to, for example `valley` and `summit`. | `valley` |
| `LIFTPASS_PASSBACK_WINDOW` | How long a pass is blocked from entering again after a successful scan. | 10 |
| `LIFTPASS_GATE_API_URL` | Base URL of the gate controller API used by `GET /gates`. | |

Set `LIFTPASS_API_TOKEN=changeme` in your local `.env` to authenticate against the gate controller.
