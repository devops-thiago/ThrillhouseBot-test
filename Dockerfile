FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/CertWatch/CertWatch.csproj src/CertWatch/
RUN dotnet restore src/CertWatch/CertWatch.csproj

COPY src/CertWatch/ src/CertWatch/
RUN dotnet publish src/CertWatch/CertWatch.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app

COPY --from=build /app/publish .

ENV CERTWATCH_EXPIRY_THRESHOLD_DAYS=30

ENTRYPOINT ["dotnet", "CertWatch.dll"]
