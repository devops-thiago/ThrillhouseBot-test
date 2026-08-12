using WebhookRelay.Models;
using WebhookRelay.Services;
using Xunit;

namespace WebhookRelay.Tests;

public class WebhookDeliveryServiceTests
{
    private class FakeWebhookSender : IWebhookSender
    {
        public Task<DeliveryResult> SendAsync(string url, string payload, string signature, CancellationToken ct)
            => Task.FromResult(new DeliveryResult(true, 200, null));
    }

    // Stands in for HmacWebhookSigner so tests don't need to compute a real HMAC digest.
    private class FakeWebhookSigner : IWebhookSigner
    {
        public string Sign(string payload, string secret) => "test-signature";
    }

    [Fact]
    public async Task DeliverAsync_SucceedsForASubscriptionWithoutASecretConfigured()
    {
        var subscription = new WebhookSubscription
        {
            Id = Guid.NewGuid(),
            Url = "https://example.com/hook",
            EventType = "order.created",
            Secret = string.Empty,
            MaxRetryAttempts = 3
        };
        var service = new WebhookDeliveryService(new FakeWebhookSender(), new FakeWebhookSigner());

        var attempt = await service.DeliverAsync(subscription, "{\"orderId\":42}", CancellationToken.None);

        Assert.Equal(DeliveryStatus.Succeeded, attempt.Status);
    }

    [Fact]
    public async Task DeliverAsync_MarksTheAttemptWithTheSubscriptionsEventType()
    {
        var subscription = new WebhookSubscription
        {
            Id = Guid.NewGuid(),
            Url = "https://example.com/hook",
            EventType = "order.cancelled",
            Secret = "shh",
            MaxRetryAttempts = 3
        };
        var service = new WebhookDeliveryService(new FakeWebhookSender(), new FakeWebhookSigner());

        var attempt = await service.DeliverAsync(subscription, "{\"orderId\":7}", CancellationToken.None);

        Assert.Equal("order.cancelled", attempt.EventType);
    }
}
