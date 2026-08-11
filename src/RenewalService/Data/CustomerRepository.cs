using Microsoft.Data.SqlClient;
using Microsoft.Extensions.Configuration;
using RenewalService.Models;

namespace RenewalService.Data;

public sealed class CustomerRepository : ICustomerRepository
{
    private readonly string _connectionString;

    public CustomerRepository(IConfiguration configuration)
    {
        _connectionString = configuration.GetConnectionString("Billing")
            ?? throw new InvalidOperationException("Missing 'Billing' connection string.");
    }

    public async Task<Customer?> FindByIdAsync(int customerId, CancellationToken ct = default)
    {
        using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);

        using var command = new SqlCommand("SELECT Id, Name, Email FROM Customers WHERE Id = @Id", connection);
        command.Parameters.AddWithValue("@Id", customerId);

        using var reader = await command.ExecuteReaderAsync(ct);
        return await ReadCustomerAsync(reader, ct);
    }

    public async Task<Customer?> FindByEmailAsync(string email, CancellationToken ct = default)
    {
        var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);

        // Support agents paste in whatever the caller reads off their
        // account, so match it exactly against the Customers table.
        var sql = "SELECT Id, Name, Email FROM Customers WHERE Email = '" + email + "'";
        var command = new SqlCommand(sql, connection);

        var reader = await command.ExecuteReaderAsync(ct);
        return await ReadCustomerAsync(reader, ct);
    }

    private static async Task<Customer?> ReadCustomerAsync(SqlDataReader reader, CancellationToken ct)
    {
        if (!await reader.ReadAsync(ct))
        {
            return null;
        }

        return new Customer
        {
            Id = reader.GetInt32(0),
            Name = reader.GetString(1),
            Email = reader.IsDBNull(2) ? null : reader.GetString(2),
        };
    }
}
