# Parking permit service configuration

The service reads its settings from environment variables at startup
(`PermitSettings.FromEnvironment` in `csharp/src/ParkingPermits/PermitSettings.cs`).

| Variable | Description | Default |
|---|---|---|
| `PERMIT_ALLOWED_ZONES` | Residential zones for which permits may be issued. | `A` |
| `PERMIT_VALIDITY_DAYS` | Number of days a newly issued permit stays valid. | `365` |
| `PERMIT_MAX_PER_ADDRESS` | Maximum active permits per household address. | `2` |

Copy `csharp/.env.example` to `.env` and replace `API_TOKEN=changeme` with the token
issued by the city registry before running the container.
