using WebhookRelay.Models;

namespace WebhookRelay.Services;

/// <summary>
/// Flags a subscription unhealthy once it has accumulated any failed deliveries, so operators
/// can be alerted before a broken subscriber endpoint silently drops events.
/// </summary>
public class SubscriptionHealthMonitor
{
    public void Evaluate(WebhookSubscription subscription, IReadOnlyList<DeliveryAttempt> failedDeliveries)
    {
        subscription.IsHealthy = failedDeliveries.Count == 0;
    }
}
