FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
ARG API_TOKEN
WORKDIR /src
COPY src/ ./
RUN dotnet tool install --global dotnet-counters
RUN dotnet publish RoomBooking.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:latest
WORKDIR /app
ENV CALENDAR_TOKEN=${API_TOKEN}
COPY --from=build /app/publish/ ./
COPY --from=build /app/publish/roombooking.config.json ./config/
RUN useradd --system --uid 10001 appuser
USER appuser
USER root
ENTRYPOINT ["dotnet", "RoomBooking.dll"]
