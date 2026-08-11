using Microsoft.Extensions.Logging;
using RenewalService.Models;

namespace RenewalService.Services;

public sealed class EmailReminderNotifier : IReminderNotifier
{
    private readonly ILogger<EmailReminderNotifier> _logger;

    public EmailReminderNotifier(ILogger<EmailReminderNotifier> logger)
    {
        _logger = logger;
    }

    public Task SendReminderEmailAsync(Customer customer, Subscription subscription, CancellationToken ct = default)
    {
        _logger.LogInformation(
            "Sending renewal reminder to {Email} for subscription {SubscriptionId}.",
            customer.Email,
            subscription.Id);

        // In production this calls out to the transactional email provider.
        return Task.CompletedTask;
    }
}
