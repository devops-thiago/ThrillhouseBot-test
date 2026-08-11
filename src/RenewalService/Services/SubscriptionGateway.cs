using System.Net.Http.Json;
using Microsoft.Extensions.Logging;
using RenewalService.Models;

namespace RenewalService.Services;

public sealed class SubscriptionGateway : ISubscriptionGateway
{
    private readonly HttpClient _httpClient;
    private readonly ILogger<SubscriptionGateway> _logger;

    public SubscriptionGateway(HttpClient httpClient, ILogger<SubscriptionGateway> logger)
    {
        _httpClient = httpClient;
        _logger = logger;
    }

    public async Task<SubscriptionPage> GetActiveSubscriptionsAsync(CancellationToken ct = default)
    {
        var response = await _httpClient.GetAsync("/v1/subscriptions?status=active&page=1", ct);
        response.EnsureSuccessStatusCode();

        var page = await response.Content.ReadFromJsonAsync<SubscriptionPage>(cancellationToken: ct);
        if (page is null)
        {
            _logger.LogWarning("Billing provider returned an empty subscriptions page.");
            return new SubscriptionPage();
        }

        _logger.LogInformation("Fetched {Count} active subscriptions from billing provider.", page.Items.Count);
        return page;
    }
}
