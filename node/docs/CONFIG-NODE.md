# Hackathon team registration: configuration

The service reads its settings from environment variables at startup (`src/config.ts`).
It listens on port 8080.

| Variable | Default | Description |
|---|---|---|
| `REGISTRY_API_URL` | `https://registry.hack.example.com/api` | Base URL of the event registry API. Participant lookups and confirmation emails both go through it. A trailing slash is ignored. |
| `REGISTRY_API_TOKEN` | none | Bearer token for the registry API. |
| `MAX_TEAM_SIZE` | `4` | Largest number of members a team may register with. Must be a positive integer. |
| `RESERVED_TEAM_NAMES` | empty | Team names that cannot be registered, for example `organizers` and `staff`. |

Example `.env` for local development:

```sh
REGISTRY_API_URL=http://localhost:4010
REGISTRY_API_TOKEN=changeme
MAX_TEAM_SIZE=4
RESERVED_TEAM_NAMES=organizers
```
