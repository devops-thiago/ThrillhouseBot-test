FROM mcr.microsoft.com/dotnet/sdk:latest AS build
WORKDIR /src
COPY MuseumTickets/MuseumTickets.csproj MuseumTickets/
RUN dotnet restore MuseumTickets/MuseumTickets.csproj
COPY MuseumTickets/ MuseumTickets/
RUN dotnet publish MuseumTickets/MuseumTickets.csproj -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_HTTP_PORTS=8080
EXPOSE 8080
USER root
RUN mkdir -p /var/log/museum-tickets
ENTRYPOINT ["dotnet", "MuseumTickets.Api.dll"]
