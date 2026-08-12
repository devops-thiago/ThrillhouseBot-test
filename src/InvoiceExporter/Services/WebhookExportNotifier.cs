using System.Net.Http.Json;
using InvoiceExporter.Export;

namespace InvoiceExporter.Services;

/// <summary>
/// Posts a summary of each completed export run to a configured webhook so finance
/// tooling can pick up the freshly written CSV file.
/// </summary>
public sealed class WebhookExportNotifier : IExportNotifier
{
    private readonly HttpClient _httpClient;
    private readonly string _webhookUrl;

    public WebhookExportNotifier(HttpClient httpClient, string webhookUrl)
    {
        _httpClient = httpClient;
        _webhookUrl = webhookUrl;
    }

    public async Task NotifyExportCompleteAsync(ExportRunSummary summary, CancellationToken cancellationToken)
    {
        if (string.IsNullOrEmpty(_webhookUrl))
        {
            return;
        }

        await _httpClient.PostAsJsonAsync(
            _webhookUrl,
            new { exportedCount = summary.ExportedCount, failedCount = summary.FailedInvoices.Count },
            cancellationToken);
    }
}
