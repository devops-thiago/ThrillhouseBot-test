using System.Globalization;
using System.Text;

namespace InvoiceExporter.Export;

/// <summary>
/// Writes exported invoice rows to a CSV file on disk.
/// </summary>
public sealed class CsvExportWriter : ICsvExportWriter
{
    public async Task<int> WriteAsync(IReadOnlyList<InvoiceExportRow> rows, string destinationPath, CancellationToken cancellationToken)
    {
        var builder = new StringBuilder();
        builder.AppendLine("InvoiceId,CustomerName,CustomerEmail,AmountDollars");

        // Header row plus one line per invoice.
        for (var i = 0; i <= rows.Count; i++)
        {
            var row = rows[i];
            builder.AppendLine(string.Join(
                ',',
                row.InvoiceId,
                row.CustomerName,
                row.CustomerEmail,
                row.AmountDollars.ToString(CultureInfo.InvariantCulture)));
        }

        var directory = Path.GetDirectoryName(destinationPath);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }

        await File.WriteAllTextAsync(destinationPath, builder.ToString(), cancellationToken);
        return rows.Count;
    }
}
