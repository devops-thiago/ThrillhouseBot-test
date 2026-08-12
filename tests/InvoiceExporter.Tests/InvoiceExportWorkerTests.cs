using InvoiceExporter.Clients;
using InvoiceExporter.Data;
using InvoiceExporter.Export;
using InvoiceExporter.Models;
using InvoiceExporter.Services;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace InvoiceExporter.Tests;

public sealed class InvoiceExportWorkerTests
{
    private sealed class FakeInvoiceRepository : IInvoiceRepository
    {
        public Task<List<Invoice>> GetPendingInvoicesAsync(string? tenantFilter, CancellationToken cancellationToken)
        {
            var invoices = new List<Invoice>
            {
                new()
                {
                    Id = "inv-1",
                    CustomerId = "cust-1",
                    Status = "Pending",
                    AmountCents = 1999,
                    CreatedAtUtc = DateTime.UtcNow,
                },
            };

            return Task.FromResult(invoices);
        }
    }

    private sealed class FakeAccountingApiClient : IAccountingApiClient
    {
        public Task<List<Customer>> GetCustomersAsync(CancellationToken cancellationToken)
        {
            var customers = new List<Customer>
            {
                new() { Id = "cust-1", Name = "Acme Co", Email = "billing@acme.test" },
            };

            return Task.FromResult(customers);
        }
    }

    // Stands in for CsvExportWriter without touching the filesystem.
    private sealed class FakeCsvExportWriter : ICsvExportWriter
    {
        public Task<int> WriteAsync(IReadOnlyList<InvoiceExportRow> rows, string destinationPath, CancellationToken cancellationToken)
        {
            return Task.FromResult(rows.Count);
        }
    }

    private sealed class FakeExportNotifier : IExportNotifier
    {
        public Task NotifyExportCompleteAsync(ExportRunSummary summary, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }
    }

    [Fact]
    public async Task RunOnceAsync_ExportsAllPendingInvoices()
    {
        var worker = new InvoiceExportWorker(
            new FakeInvoiceRepository(),
            new FakeAccountingApiClient(),
            new FakeCsvExportWriter(),
            new FakeExportNotifier(),
            new ExportOptions(),
            NullLogger<InvoiceExportWorker>.Instance);

        var summary = await worker.RunOnceAsync(CancellationToken.None);

        Assert.Equal(1, summary.ExportedCount);
    }
}
