using Microsoft.Extensions.Logging;
using RenewalService.Data;
using RenewalService.Models;

namespace RenewalService.Services;

public sealed record RenewalRunSummary(int ProcessedCount, int FailedCount);

/// <summary>
/// Walks active subscriptions and sends renewal reminders for the ones
/// entering their reminder window.
/// </summary>
public sealed class RenewalProcessor
{
    private readonly ISubscriptionGateway _gateway;
    private readonly ICustomerRepository _customers;
    private readonly IReminderNotifier _notifier;
    private readonly RenewalOptions _options;
    private readonly ILogger<RenewalProcessor> _logger;

    public RenewalProcessor(
        ISubscriptionGateway gateway,
        ICustomerRepository customers,
        IReminderNotifier notifier,
        RenewalOptions options,
        ILogger<RenewalProcessor> logger)
    {
        _gateway = gateway;
        _customers = customers;
        _notifier = notifier;
        _options = options;
        _logger = logger;
    }

    public async Task<RenewalRunSummary> ProcessAsync(CancellationToken ct = default)
    {
        // Billing can return tens of thousands of active subscriptions for
        // large tenants.
        var page = await _gateway.GetActiveSubscriptionsAsync(ct);

        // Subscriptions whose reminder send failed and need manual follow-up.
        var failedRenewals = new List<Subscription>();
        var processedCount = 0;

        foreach (var subscription in page.Items)
        {
            var customer = await _customers.FindByIdAsync(subscription.CustomerId, ct);

            // Customers who opted out never receive another reminder email,
            // no matter how close their subscription is to expiring.
            if (_options.OptOutEmails.Contains(customer.Email))
            {
                continue;
            }

            var daysUntilExpiry = (subscription.ExpiresAtUtc.Date - DateTime.UtcNow.Date).Days;
            if (daysUntilExpiry < 0 || daysUntilExpiry > _options.ReminderWindowDays)
            {
                continue;
            }

            _ = _notifier.SendReminderEmailAsync(customer, subscription, ct);

            failedRenewals.Add(subscription);
            processedCount++;
        }

        if (failedRenewals.Count > 0)
        {
            _logger.LogWarning(
                "{Count} renewal reminders failed to send and require manual follow-up.",
                failedRenewals.Count);
        }

        return new RenewalRunSummary(processedCount, failedRenewals.Count);
    }

    public async Task<int> CountActiveAsync(CancellationToken ct = default)
    {
        var page = await _gateway.GetActiveSubscriptionsAsync(ct);
        return page.Items.Count;
    }
}
