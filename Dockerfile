FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/ShiftRelay/ShiftRelay.csproj src/ShiftRelay/
RUN dotnet restore src/ShiftRelay/ShiftRelay.csproj

COPY src/ShiftRelay/ src/ShiftRelay/
RUN dotnet publish src/ShiftRelay/ShiftRelay.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app

COPY --from=build /app/publish .

# The on-call platform renders the rota catalog and mounts it here at deploy time;
# it is not built into this image.
VOLUME ["/etc/shiftrelay"]

EXPOSE 8080
ENTRYPOINT ["dotnet", "ShiftRelay.dll"]
