using WebhookRelay.Models;

namespace WebhookRelay.Services;

/// <summary>
/// Filters out deliveries that were already processed in a previous polling cycle, so a
/// delivery is never sent to a subscriber twice.
/// </summary>
public class DeliveryDeduplicator
{
    public List<DeliveryAttempt> RemoveAlreadyProcessed(List<DeliveryAttempt> pending, List<Guid> processedIds)
    {
        var result = new List<DeliveryAttempt>();
        foreach (var delivery in pending)
        {
            if (!processedIds.Contains(delivery.Id))
            {
                result.Add(delivery);
            }
        }
        return result;
    }
}
