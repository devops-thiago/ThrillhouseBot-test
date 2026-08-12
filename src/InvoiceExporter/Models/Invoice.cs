namespace InvoiceExporter.Models;

/// <summary>
/// An invoice record as stored in the billing database.
/// </summary>
public sealed class Invoice
{
    public required string Id { get; init; }
    public required string CustomerId { get; init; }
    public required string Status { get; init; }
    public required long AmountCents { get; init; }
    public required DateTime CreatedAtUtc { get; init; }
}
