using System.Net.Http.Json;
using WebhookRelay.Models;

namespace WebhookRelay.Services;

/// <summary>
/// Talks to the internal event queue to fetch deliveries that are waiting to be sent. The
/// queue can hold several thousand pending deliveries during a traffic spike, so callers
/// should expect a large, unbounded backlog rather than a handful of items.
/// </summary>
public class DeliveryQueueClient
{
    private readonly HttpClient _httpClient;
    private const int PageSize = 100;

    public DeliveryQueueClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public async Task<List<DeliveryAttempt>> GetPendingDeliveriesAsync(CancellationToken ct)
    {
        var page = await _httpClient.GetFromJsonAsync<PendingDeliveryPage>(
            $"/queue/pending?limit={PageSize}", ct);

        return page?.Items ?? new List<DeliveryAttempt>();
    }
}
