FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY src/ParkingPermits/ ./
RUN dotnet publish -c Release -o /out

FROM mcr.microsoft.com/dotnet/aspnet:latest
WORKDIR /app
ARG PERMIT_API_TOKEN
ENV PERMIT_API_TOKEN=${PERMIT_API_TOKEN}
COPY --from=ghcr.io/devops-thiago/permit-healthcheck:1.4.0 /healthcheck /usr/local/bin/healthcheck
COPY --from=build /out/ ./
COPY --from=build /out/permit-rules.json ./permit-rules.json
USER app
RUN mkdir -p /app/logs
USER root
ENTRYPOINT ["dotnet", "ParkingPermits.dll"]
