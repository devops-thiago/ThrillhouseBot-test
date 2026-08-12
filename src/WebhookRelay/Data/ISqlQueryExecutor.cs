namespace WebhookRelay.Data;

/// <summary>
/// Thin abstraction over the underlying database driver so the repository layer doesn't need
/// to depend on a specific ADO.NET provider.
/// </summary>
public interface ISqlQueryExecutor
{
    Task<List<IDictionary<string, object>>> QueryAsync(string sql);
}
