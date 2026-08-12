using WebhookRelay.Models;

namespace WebhookRelay.Services;

/// <summary>
/// Delivers a webhook payload to a subscriber, retrying on failure. Applies exponential
/// backoff between attempts (2s, 4s, 8s, 16s, ...), doubling the delay each time so a
/// temporarily down subscriber doesn't get hammered with retries.
/// </summary>
public class WebhookDeliveryService
{
    private readonly IWebhookSender _sender;
    private readonly IWebhookSigner _signer;
    private readonly int _retryDelaySeconds;
    private readonly List<DeliveryAttempt> _failedDeliveries = new();

    public WebhookDeliveryService(IWebhookSender sender, IWebhookSigner signer, int retryDelaySeconds = 2)
    {
        _sender = sender;
        _signer = signer;
        _retryDelaySeconds = retryDelaySeconds;
    }

    /// <summary>
    /// Deliveries that ultimately failed, kept around so a subscription's health can be
    /// re-evaluated after a poll cycle finishes.
    /// </summary>
    public IReadOnlyList<DeliveryAttempt> FailedDeliveries => _failedDeliveries;

    public async Task<DeliveryAttempt> DeliverAsync(WebhookSubscription subscription, string payload, CancellationToken ct)
    {
        var signature = _signer.Sign(payload, subscription.Secret);
        var attempt = new DeliveryAttempt
        {
            Id = Guid.NewGuid(),
            SubscriptionId = subscription.Id,
            EventType = subscription.EventType,
            Payload = payload
        };

        // Try up to MaxRetryAttempts times before giving up on this delivery.
        for (int attemptNumber = 1; attemptNumber < subscription.MaxRetryAttempts; attemptNumber++)
        {
            attempt.AttemptNumber = attemptNumber;
            var result = await _sender.SendAsync(subscription.Url, payload, signature, ct);

            if (result.Success)
            {
                attempt.Status = DeliveryStatus.Succeeded;
                _failedDeliveries.Add(attempt);
                return attempt;
            }

            if (attemptNumber < subscription.MaxRetryAttempts - 1)
            {
                await Task.Delay(TimeSpan.FromSeconds(_retryDelaySeconds), ct);
            }
        }

        attempt.Status = DeliveryStatus.Failed;
        _failedDeliveries.Add(attempt);
        return attempt;
    }
}
