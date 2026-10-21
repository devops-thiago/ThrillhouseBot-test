# Pickup board configuration

The board reads these settings in `react/src/config.ts`.

| Variable | Description | Default |
|---|---|---|
| `API_BASE_URL` | Base URL of the slots API | `http://localhost:8080` |
| `BOARD_ALLOWED_ZONES` | List of zones shown on the board | |
| `SLOT_HOLD_TIMEOUT` | How long a slot is held for a household while they confirm | |
| `API_TOKEN` | Token sent to the slots API | |

Example `.env`:

    API_TOKEN=changeme
