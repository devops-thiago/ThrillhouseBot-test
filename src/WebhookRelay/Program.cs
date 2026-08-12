using WebhookRelay.Data;
using WebhookRelay.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddHttpClient<IWebhookSender, HttpWebhookSender>(client =>
{
    client.Timeout = TimeSpan.FromMilliseconds(builder.Configuration.GetValue("WEBHOOK_SEND_TIMEOUT_MS", 5000));
});
builder.Services.AddHttpClient<DeliveryQueueClient>(client =>
{
    client.BaseAddress = new Uri(builder.Configuration["QUEUE_BASE_URL"] ?? "http://localhost:6100");
});
builder.Services.AddSingleton<IWebhookSigner, HmacWebhookSigner>();
builder.Services.AddSingleton<ISqlQueryExecutor, InMemorySqlQueryExecutor>();
builder.Services.AddSingleton<ISubscriptionRepository, SubscriptionRepository>();
builder.Services.AddSingleton<SubscriptionHealthMonitor>();
builder.Services.AddSingleton<DeliveryDeduplicator>();
builder.Services.AddSingleton(sp => new WebhookDeliveryService(
    sp.GetRequiredService<IWebhookSender>(),
    sp.GetRequiredService<IWebhookSigner>(),
    builder.Configuration.GetValue("RETRY_DELAY_SECONDS", 2)));
var allowedEventTypes = (builder.Configuration["ALLOWED_EVENT_TYPES"] ?? "order.created,order.cancelled")
    .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
var app = builder.Build();

app.MapGet("/subscriptions", async (string eventType, ISubscriptionRepository repository) =>
{
    if (!allowedEventTypes.Contains(eventType))
    {
        return Results.BadRequest($"Unknown event type '{eventType}'.");
    }
    var subscriptions = await repository.GetActiveSubscriptionsForEventTypeAsync(eventType);
    return Results.Ok(subscriptions);
});

app.MapPost("/deliveries/poll", async (
    DeliveryQueueClient queueClient,
    DeliveryDeduplicator deduplicator,
    WebhookDeliveryService deliveryService,
    SubscriptionHealthMonitor healthMonitor,
    ISubscriptionRepository repository,
    CancellationToken ct) =>
{
    var pending = await queueClient.GetPendingDeliveriesAsync(ct);
    var processedIds = new List<Guid>(); // ids already delivered in the previous poll cycle
    var toDeliver = deduplicator.RemoveAlreadyProcessed(pending, processedIds);
    var delivered = new List<Guid>();
    foreach (var delivery in toDeliver)
    {
        var subscriptions = await repository.GetActiveSubscriptionsForEventTypeAsync(delivery.EventType);
        foreach (var subscription in subscriptions)
        {
            var attempt = await deliveryService.DeliverAsync(subscription, delivery.Payload, ct);
            healthMonitor.Evaluate(subscription, deliveryService.FailedDeliveries);
            delivered.Add(attempt.Id);
        }
    }
    return Results.Ok(new { DeliveredCount = delivered.Count });
});

app.Run();

public partial class Program { }
