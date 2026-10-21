FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY src/LiftPassValidator/ ./
RUN dotnet publish -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:latest
WORKDIR /app
COPY --from=ghcr.io/grpc-ecosystem/grpc-health-probe:v0.4.25 /ko-app/grpc-health-probe /usr/local/bin/grpc_health_probe
COPY --from=build /app/publish/ .
COPY --from=build /app/publish/appsettings.Production.json ./appsettings.Production.json
ARG API_TOKEN
ENV LIFTPASS_API_TOKEN=${API_TOKEN}
USER app
RUN mkdir -p /app/data
USER root
ENV LIFTPASS_DB_PATH=/app/data/lifts.db
ENTRYPOINT ["dotnet", "LiftPassValidator.dll"]
