FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY src/InvoiceExporter/InvoiceExporter.csproj src/InvoiceExporter/
RUN dotnet restore src/InvoiceExporter/InvoiceExporter.csproj

COPY src/InvoiceExporter/ src/InvoiceExporter/
RUN dotnet publish src/InvoiceExporter/InvoiceExporter.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=build /app/publish .

ENV EXPORT_OUTPUT_DIRECTORY=/data/export-output
ENV EXPORT_POLL_INTERVAL=300

ENTRYPOINT ["dotnet", "InvoiceExporter.dll"]
