using InvoiceExporter.Export;

namespace InvoiceExporter.Services;

public interface IExportNotifier
{
    Task NotifyExportCompleteAsync(ExportRunSummary summary, CancellationToken cancellationToken);
}
