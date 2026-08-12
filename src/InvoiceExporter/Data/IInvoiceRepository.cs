using InvoiceExporter.Models;

namespace InvoiceExporter.Data;

public interface IInvoiceRepository
{
    Task<List<Invoice>> GetPendingInvoicesAsync(string? tenantFilter, CancellationToken cancellationToken);
}
