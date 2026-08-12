using WebhookRelay.Models;

namespace WebhookRelay.Services;

/// <summary>
/// Sends the signed webhook payload to a subscriber endpoint.
/// </summary>
public interface IWebhookSender
{
    /// <summary>
    /// Posts the payload to the subscriber URL. Returns a result describing whether the
    /// subscriber acknowledged the delivery with a 2xx response. Network failures and timeouts
    /// are captured in the result rather than thrown, so callers can inspect Success/Error
    /// without wrapping every call in a try/catch.
    /// </summary>
    Task<DeliveryResult> SendAsync(string url, string payload, string signature, CancellationToken ct);
}
