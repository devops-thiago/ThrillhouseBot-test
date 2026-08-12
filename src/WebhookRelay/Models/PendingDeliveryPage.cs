namespace WebhookRelay.Models;

/// <summary>
/// One page of results from the internal event queue's pending-deliveries endpoint.
/// </summary>
public class PendingDeliveryPage
{
    public required List<DeliveryAttempt> Items { get; init; }
    public string? NextCursor { get; init; }
}
