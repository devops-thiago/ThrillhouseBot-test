namespace InvoiceExporter.Export;

public interface ICsvExportWriter
{
    Task<int> WriteAsync(IReadOnlyList<InvoiceExportRow> rows, string destinationPath, CancellationToken cancellationToken);
}
