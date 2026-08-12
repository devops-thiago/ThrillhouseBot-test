namespace InvoiceExporter.Export;

/// <summary>
/// A single flattened row destined for the CSV export file.
/// </summary>
public sealed class InvoiceExportRow
{
    public required string InvoiceId { get; init; }
    public required string CustomerName { get; init; }
    public required string CustomerEmail { get; init; }
    public required decimal AmountDollars { get; init; }
}
