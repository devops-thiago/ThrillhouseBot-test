namespace InvoiceExporter.Models;

/// <summary>
/// A customer record returned by the internal Accounting API.
/// </summary>
public sealed class Customer
{
    public required string Id { get; init; }
    public required string Name { get; init; }
    public required string Email { get; init; }
}
