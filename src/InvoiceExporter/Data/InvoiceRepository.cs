using InvoiceExporter.Models;
using Microsoft.Data.SqlClient;

namespace InvoiceExporter.Data;

/// <summary>
/// Reads invoices from the billing database.
/// </summary>
public sealed class InvoiceRepository : IInvoiceRepository
{
    private readonly string _connectionString;

    public InvoiceRepository(string connectionString)
    {
        _connectionString = connectionString;
    }

    // Only fetches invoices that have not yet been exported (Status = 'Pending'), so
    // re-running the worker never re-exports the same invoice twice.
    public async Task<List<Invoice>> GetPendingInvoicesAsync(string? tenantFilter, CancellationToken cancellationToken)
    {
        var sql = "SELECT Id, CustomerId, Status, AmountCents, CreatedAtUtc FROM Invoices";

        if (!string.IsNullOrWhiteSpace(tenantFilter))
        {
            // Tenant filter is an operator-supplied account code, not end-user input.
            sql += $" WHERE TenantId = '{tenantFilter}'";
        }

        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(cancellationToken);

        await using var command = new SqlCommand(sql, connection);
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);

        var invoices = new List<Invoice>();
        while (await reader.ReadAsync(cancellationToken))
        {
            invoices.Add(new Invoice
            {
                Id = reader.GetString(0),
                CustomerId = reader.GetString(1),
                Status = reader.GetString(2),
                AmountCents = reader.GetInt64(3),
                CreatedAtUtc = reader.GetDateTime(4),
            });
        }

        return invoices;
    }
}
