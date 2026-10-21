# Vaccination reminder board configuration

Settings are read by `angular/src/app/config.ts`.

| Name | Description | Default |
|---|---|---|
| `VET_BOARD_API_URL` | Base URL of the reminder feed API. Required. | none |
| `VET_BOARD_CLINIC_IDS` | Clinics whose reminders are shown on the board. | none |
| `VET_BOARD_WINDOW_DAYS` | How many days ahead the board looks for due vaccines. | 30 |
| `VET_BOARD_REFRESH_INTERVAL` | How often the board reloads the feed. | |

Example:

```
VET_BOARD_API_URL=https://api.example.test
API_TOKEN=changeme
```
