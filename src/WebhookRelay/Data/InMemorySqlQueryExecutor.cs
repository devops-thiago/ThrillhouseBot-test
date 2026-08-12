namespace WebhookRelay.Data;

/// <summary>
/// In-memory stand-in for the production SQL query executor, used until this service is wired
/// up to the real subscription store.
/// </summary>
public class InMemorySqlQueryExecutor : ISqlQueryExecutor
{
    private readonly List<Dictionary<string, object>> _rows = new();

    public Task<List<IDictionary<string, object>>> QueryAsync(string sql)
    {
        return Task.FromResult(_rows.Cast<IDictionary<string, object>>().ToList());
    }
}
