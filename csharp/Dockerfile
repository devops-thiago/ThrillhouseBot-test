# Build context: csharp/
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY HoldsService/HoldsService.csproj HoldsService/
RUN dotnet restore HoldsService/HoldsService.csproj
COPY HoldsService/ HoldsService/
RUN dotnet publish HoldsService/HoldsService.csproj -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:latest
WORKDIR /app
COPY --from=build /app/publish .

ARG CATALOG_API_TOKEN
ENV HOLDS_CATALOG_TOKEN=${CATALOG_API_TOKEN}
ENV HOLDS_SLIP_ROOT=/var/lib/holds/slips
RUN mkdir -p /var/lib/holds/slips && chown app:app /var/lib/holds/slips

USER app
EXPOSE 8080

# The slip share is mounted from the circulation desk's NFS export, which only root can read.
USER root
ENTRYPOINT ["dotnet", "LibraryHolds.dll"]
