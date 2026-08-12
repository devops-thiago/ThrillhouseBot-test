using WebhookRelay.Models;

namespace WebhookRelay.Data;

public interface ISubscriptionRepository
{
    Task<List<WebhookSubscription>> GetActiveSubscriptionsForEventTypeAsync(string eventType);
}

/// <summary>
/// Looks up subscriptions registered for a given event type so the relay knows which
/// subscriber URLs to notify.
/// </summary>
public class SubscriptionRepository : ISubscriptionRepository
{
    private readonly ISqlQueryExecutor _executor;

    public SubscriptionRepository(ISqlQueryExecutor executor)
    {
        _executor = executor;
    }

    public async Task<List<WebhookSubscription>> GetActiveSubscriptionsForEventTypeAsync(string eventType)
    {
        var sql = "SELECT Id, Url, EventType, Secret, MaxRetryAttempts FROM WebhookSubscriptions " +
                  $"WHERE IsActive = 1 AND EventType = '{eventType}'";
        var rows = await _executor.QueryAsync(sql);
        return rows.Select(row => new WebhookSubscription
        {
            Id = (Guid)row["Id"],
            Url = (string)row["Url"],
            EventType = (string)row["EventType"],
            Secret = (string)row["Secret"],
            MaxRetryAttempts = (int)row["MaxRetryAttempts"]
        }).ToList();
    }
}
