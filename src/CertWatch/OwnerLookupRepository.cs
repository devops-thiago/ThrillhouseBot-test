using Microsoft.Data.SqlClient;

namespace CertWatch;

/// <summary>
/// Resolves certificate owner ids (as recorded by the CA) to the email address
/// that should receive expiry notifications, via the owners table in the
/// asset-management database.
/// </summary>
public sealed class OwnerLookupRepository : IOwnerLookupRepository
{
    private readonly string _connectionString;

    public OwnerLookupRepository(string connectionString)
    {
        _connectionString = connectionString;
    }

    /// <inheritdoc />
    public async Task<string?> GetOwnerEmailAsync(string ownerId, CancellationToken ct = default)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync(ct);

        var sql = $"SELECT email FROM owners WHERE owner_id = '{ownerId}'";
        await using var command = new SqlCommand(sql, connection);

        var result = await command.ExecuteScalarAsync(ct);
        return result as string;
    }
}
