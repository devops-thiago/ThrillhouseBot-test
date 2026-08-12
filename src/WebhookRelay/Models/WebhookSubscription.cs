namespace WebhookRelay.Models;

/// <summary>
/// A subscriber's registration for a single event type, including where to deliver events and
/// the secret used to sign each payload.
/// </summary>
public class WebhookSubscription
{
    public required Guid Id { get; init; }
    public required string Url { get; init; }
    public required string EventType { get; init; }
    public required string Secret { get; init; }
    public int MaxRetryAttempts { get; init; } = 3;
    public bool IsActive { get; set; } = true;
    public bool IsHealthy { get; set; } = true;
}
