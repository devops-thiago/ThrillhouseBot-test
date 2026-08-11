using Microsoft.Extensions.Logging.Abstractions;
using RenewalService.Data;
using RenewalService.Models;
using RenewalService.Services;
using Xunit;

namespace RenewalService.Tests;

public class RenewalProcessorTests
{
    [Fact]
    public async Task ProcessAsync_SendsReminder_ForSubscriptionDueSoon()
    {
        var subscription = new Subscription
        {
            Id = 1,
            CustomerId = 42,
            PlanCode = "PRO",
            ExpiresAtUtc = DateTime.UtcNow.AddDays(3),
            AutoRenew = false,
        };

        var gateway = new FakeSubscriptionGateway(new SubscriptionPage
        {
            Items = new List<Subscription> { subscription },
            NextPageToken = null,
        });

        var customers = new FakeCustomerRepository(new Customer { Id = 42, Name = "Ada", Email = "ada@example.com" });
        var notifier = new FakeNotifier();
        var options = new RenewalOptions { ReminderWindowDays = 7, OptOutEmails = new List<string>() };
        var processor = new RenewalProcessor(gateway, customers, notifier, options, NullLogger<RenewalProcessor>.Instance);

        var summary = await processor.ProcessAsync();

        Assert.Equal(1, summary.ProcessedCount);
    }

    private sealed class FakeSubscriptionGateway : ISubscriptionGateway
    {
        private readonly SubscriptionPage _page;

        public FakeSubscriptionGateway(SubscriptionPage page) => _page = page;

        public Task<SubscriptionPage> GetActiveSubscriptionsAsync(CancellationToken ct = default) => Task.FromResult(_page);
    }

    private sealed class FakeCustomerRepository : ICustomerRepository
    {
        private readonly Customer _customer;

        public FakeCustomerRepository(Customer customer) => _customer = customer;

        // Always returns a customer, never null -- ICustomerRepository.FindByIdAsync's
        // own contract says it returns null for an orphaned subscription, so this
        // fake can never exercise that path.
        public Task<Customer?> FindByIdAsync(int customerId, CancellationToken ct = default) => Task.FromResult<Customer?>(_customer);

        public Task<Customer?> FindByEmailAsync(string email, CancellationToken ct = default) => Task.FromResult<Customer?>(_customer);
    }

    private sealed class FakeNotifier : IReminderNotifier
    {
        public Task SendReminderEmailAsync(Customer customer, Subscription subscription, CancellationToken ct = default) => Task.CompletedTask;
    }
}
