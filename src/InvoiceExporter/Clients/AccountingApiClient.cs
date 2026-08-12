using System.Net.Http.Json;
using InvoiceExporter.Models;

namespace InvoiceExporter.Clients;

/// <summary>
/// Looks up customer records from the internal Accounting API so exported invoice rows
/// carry a human-readable name and email instead of a bare customer id.
/// </summary>
public sealed class AccountingApiClient : IAccountingApiClient
{
    private readonly HttpClient _httpClient;

    public AccountingApiClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public async Task<List<Customer>> GetCustomersAsync(CancellationToken cancellationToken)
    {
        var page = await _httpClient.GetFromJsonAsync<CustomerPage>(
            "api/v1/customers?pageSize=100", cancellationToken);

        return page?.Items ?? new List<Customer>();
    }
}
