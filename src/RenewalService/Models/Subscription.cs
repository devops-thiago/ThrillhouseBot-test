namespace RenewalService.Models;

public sealed class Subscription
{
    public int Id { get; init; }

    public int CustomerId { get; init; }

    public string PlanCode { get; init; } = string.Empty;

    public DateTime ExpiresAtUtc { get; init; }

    public bool AutoRenew { get; init; }
}
