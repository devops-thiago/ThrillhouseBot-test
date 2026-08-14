FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/RoomBookingService/RoomBookingService.csproj src/RoomBookingService/
RUN dotnet restore src/RoomBookingService/RoomBookingService.csproj

COPY src/RoomBookingService/ src/RoomBookingService/
RUN dotnet publish src/RoomBookingService/RoomBookingService.csproj -c Release -o /app/out

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app

COPY --from=build /app/publish .

# The facilities platform publishes the room catalog and mounts it here at deploy
# time; it is not built into this image.
VOLUME ["/etc/roombooking"]

EXPOSE 8080
ENTRYPOINT ["dotnet", "RoomBookingService.dll"]
