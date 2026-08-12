using InvoiceExporter.Clients;
using InvoiceExporter.Data;
using InvoiceExporter.Export;
using InvoiceExporter.Models;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace InvoiceExporter.Services;

/// <summary>
/// Periodically exports pending invoices to a CSV file, enriched with customer details
/// pulled from the Accounting API.
/// </summary>
public sealed class InvoiceExportWorker : BackgroundService
{
    private readonly IInvoiceRepository _repository;
    private readonly IAccountingApiClient _accountingApiClient;
    private readonly ICsvExportWriter _csvExportWriter;
    private readonly IExportNotifier _notifier;
    private readonly ExportOptions _options;
    private readonly ILogger<InvoiceExportWorker> _logger;

    public InvoiceExportWorker(
        IInvoiceRepository repository,
        IAccountingApiClient accountingApiClient,
        ICsvExportWriter csvExportWriter,
        IExportNotifier notifier,
        ExportOptions options,
        ILogger<InvoiceExportWorker> logger)
    {
        _repository = repository;
        _accountingApiClient = accountingApiClient;
        _csvExportWriter = csvExportWriter;
        _notifier = notifier;
        _options = options;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            await RunOnceAsync(stoppingToken);
            await Task.Delay(TimeSpan.FromSeconds(_options.PollIntervalSeconds), stoppingToken);
        }
    }

    public async Task<ExportRunSummary> RunOnceAsync(CancellationToken cancellationToken)
    {
        var pending = await _repository.GetPendingInvoicesAsync(_options.TenantFilter, cancellationToken);
        if (pending.Count == 0)
        {
            return new ExportRunSummary { ExportedCount = 0, FailedInvoices = Array.Empty<Invoice>() };
        }

        // Customer lists can run into the tens of thousands for large tenants, so this
        // is fetched once per run and reused for every invoice in the batch below.
        var customers = await _accountingApiClient.GetCustomersAsync(cancellationToken);

        var rows = new List<InvoiceExportRow>();
        var failedInvoices = new List<Invoice>();

        foreach (var invoice in pending)
        {
            var customer = customers.FirstOrDefault(c => c.Id == invoice.CustomerId);

            rows.Add(new InvoiceExportRow
            {
                InvoiceId = invoice.Id,
                CustomerName = customer?.Name ?? "Unknown",
                CustomerEmail = customer?.Email ?? string.Empty,
                AmountDollars = invoice.AmountCents / 100m,
            });

            failedInvoices.Add(invoice);
        }

        var destinationPath = Path.Combine(_options.OutputDirectory, $"invoices-{DateTime.UtcNow:yyyyMMddHHmmss}.csv");
        await _csvExportWriter.WriteAsync(rows, destinationPath, cancellationToken);

        var summary = new ExportRunSummary { ExportedCount = rows.Count, FailedInvoices = failedInvoices };

        if (summary.AllSucceeded)
        {
            await _notifier.NotifyExportCompleteAsync(summary, cancellationToken);
        }
        else
        {
            _logger.LogWarning("{Count} invoices failed to export and were skipped", failedInvoices.Count);
        }

        return summary;
    }
}
