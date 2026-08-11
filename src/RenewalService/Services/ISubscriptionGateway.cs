using RenewalService.Models;

namespace RenewalService.Services;

public interface ISubscriptionGateway
{
    /// <summary>
    /// Retrieves every currently active subscription from the billing
    /// provider, walking all pages until the provider stops returning a
    /// next-page token.
    /// </summary>
    Task<SubscriptionPage> GetActiveSubscriptionsAsync(CancellationToken ct = default);
}
