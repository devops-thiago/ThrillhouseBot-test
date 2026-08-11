namespace RenewalService.Models;

public sealed class Customer
{
    public int Id { get; init; }

    public string Name { get; init; } = string.Empty;

    public string? Email { get; init; }
}
