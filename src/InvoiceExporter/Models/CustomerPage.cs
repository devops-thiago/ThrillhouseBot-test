namespace InvoiceExporter.Models;

/// <summary>
/// One page of a paginated customer listing. <see cref="NextPageToken"/> is non-null
/// whenever more results are available and should be passed back to fetch the next page.
/// </summary>
public sealed class CustomerPage
{
    public List<Customer> Items { get; init; } = new();

    public string? NextPageToken { get; init; }
}
