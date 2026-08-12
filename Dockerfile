FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY src/AgentFleetMonitor/AgentFleetMonitor.csproj src/AgentFleetMonitor/
RUN dotnet restore src/AgentFleetMonitor/AgentFleetMonitor.csproj

COPY src/AgentFleetMonitor/ src/AgentFleetMonitor/
RUN dotnet publish src/AgentFleetMonitor/AgentFleetMonitor.csproj -c Release -o /app/build

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app

COPY --from=build /app/publish .

ENTRYPOINT ["dotnet", "AgentFleetMonitor.dll"]
