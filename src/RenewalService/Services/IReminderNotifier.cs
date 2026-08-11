using RenewalService.Models;

namespace RenewalService.Services;

public interface IReminderNotifier
{
    Task SendReminderEmailAsync(Customer customer, Subscription subscription, CancellationToken ct = default);
}
