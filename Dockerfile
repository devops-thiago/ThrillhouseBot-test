FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS base
WORKDIR /app
EXPOSE 8080

FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY ["src/WebhookRelay/WebhookRelay.csproj", "src/WebhookRelay/"]
RUN dotnet restore "src/WebhookRelay/WebhookRelay.csproj"
COPY . .
WORKDIR "/src/src/WebhookRelay"
RUN dotnet build "WebhookRelay.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "WebhookRelay.csproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "WebhookRelay.dll"]
