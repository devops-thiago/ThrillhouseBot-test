using InvoiceExporter.Models;

namespace InvoiceExporter.Export;

/// <summary>
/// Outcome of a single export run.
/// </summary>
public sealed class ExportRunSummary
{
    public required int ExportedCount { get; init; }

    public required IReadOnlyList<Invoice> FailedInvoices { get; init; }

    public bool AllSucceeded => FailedInvoices.Count == 0;
}
