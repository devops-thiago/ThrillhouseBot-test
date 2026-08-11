namespace RenewalService.Models;

/// <summary>
/// One page of results from the billing provider's subscriptions endpoint.
/// </summary>
public sealed class SubscriptionPage
{
    public List<Subscription> Items { get; init; } = new();

    public string? NextPageToken { get; init; }
}
