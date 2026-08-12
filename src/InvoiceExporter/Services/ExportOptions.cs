namespace InvoiceExporter.Services;

/// <summary>
/// Runtime configuration for the invoice exporter, read once at startup from environment
/// variables. See docs/CONFIG-CSHARP.md for the full reference.
/// </summary>
public sealed class ExportOptions
{
    public string? TenantFilter { get; init; }
    public int PollIntervalSeconds { get; init; } = 300;
    public string[] AllowedStatuses { get; init; } = { "Pending" };
    public string OutputDirectory { get; init; } = "./export-output";
    public string AccountingApiUrl { get; init; } = "https://accounting.internal/";
    public string WebhookUrl { get; init; } = string.Empty;

    public static ExportOptions FromEnvironment()
    {
        var allowedStatusesRaw = Environment.GetEnvironmentVariable("EXPORT_ALLOWED_STATUSES");
        var allowedStatuses = string.IsNullOrWhiteSpace(allowedStatusesRaw)
            ? new[] { "Pending" }
            : allowedStatusesRaw.Split(',', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries);

        var pollIntervalSeconds = int.TryParse(Environment.GetEnvironmentVariable("EXPORT_POLL_INTERVAL"), out var parsedInterval)
            ? parsedInterval
            : 300;

        return new ExportOptions
        {
            TenantFilter = Environment.GetEnvironmentVariable("EXPORT_TENANT_FILTER"),
            PollIntervalSeconds = pollIntervalSeconds,
            AllowedStatuses = allowedStatuses,
            OutputDirectory = Environment.GetEnvironmentVariable("EXPORT_OUTPUT_DIRECTORY") ?? "./export-output",
            AccountingApiUrl = Environment.GetEnvironmentVariable("EXPORT_ACCOUNTING_API_URL") ?? "https://accounting.internal/",
            WebhookUrl = Environment.GetEnvironmentVariable("EXPORT_WEBHOOK_URL") ?? string.Empty,
        };
    }
}
