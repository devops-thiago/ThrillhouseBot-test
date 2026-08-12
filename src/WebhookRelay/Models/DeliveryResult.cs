namespace WebhookRelay.Models;

/// <summary>
/// Outcome of a single delivery attempt to a subscriber's HTTP endpoint.
/// </summary>
public record DeliveryResult(bool Success, int? StatusCode, string? Error);
