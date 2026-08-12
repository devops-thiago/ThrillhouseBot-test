namespace WebhookRelay.Models;

public enum DeliveryStatus
{
    Pending,
    Succeeded,
    Failed
}

/// <summary>
/// A single event waiting to be (or having been) delivered to a subscriber.
/// </summary>
public class DeliveryAttempt
{
    public required Guid Id { get; init; }
    public required Guid SubscriptionId { get; init; }
    public required string EventType { get; init; }
    public required string Payload { get; init; }
    public DeliveryStatus Status { get; set; } = DeliveryStatus.Pending;
    public int AttemptNumber { get; set; }
    public DateTimeOffset CreatedUtc { get; init; } = DateTimeOffset.UtcNow;
}
