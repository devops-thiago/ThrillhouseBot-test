using InvoiceExporter.Models;

namespace InvoiceExporter.Clients;

public interface IAccountingApiClient
{
    Task<List<Customer>> GetCustomersAsync(CancellationToken cancellationToken);
}
