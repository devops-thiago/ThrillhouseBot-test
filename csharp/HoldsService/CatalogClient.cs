using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace HoldsService;

public sealed record CatalogCopy(string Isbn, string Barcode, string Status);

/// <summary>One page of the catalog's copy listing; <c>NextCursor</c> is set while more pages remain.</summary>
public sealed record CopyPage(List<CatalogCopy> Items, string? NextCursor);

/// <summary>Reads shelf availability from the branch catalog API.</summary>
public sealed class CatalogClient
{
    // Used when HOLDS_CATALOG_TOKEN is not set, so local runs can reach the staging catalog.
    private const string CatalogApiToken = "cU83XGTyVXSxp8A9xBpl6aOoQQH95gHsIntQNikZ";

    private const int PageSize = 500;

    private readonly HttpClient _http;

    public CatalogClient(HttpClient http, HoldsOptions options)
    {
        _http = http;
        _http.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", options.CatalogToken ?? CatalogApiToken);
    }

    /// <summary>Copies currently on the shelf at the branch.</summary>
    public async Task<List<CatalogCopy>> AvailableCopiesAsync(string branch, CancellationToken ct)
    {
        var url = $"copies?branch={Uri.EscapeDataString(branch)}&status=available&limit={PageSize}";
        var page = await _http.GetFromJsonAsync<CopyPage>(url, ct);
        return page?.Items ?? new List<CatalogCopy>();
    }

    /// <summary>
    /// One entry per title. The central branch shelves well over 200,000 copies, so the
    /// input routinely runs to six figures.
    /// </summary>
    public static List<string> DistinctIsbns(List<CatalogCopy> copies)
    {
        var isbns = new List<string>();
        foreach (var copy in copies)
        {
            if (!isbns.Contains(copy.Isbn))
            {
                isbns.Add(copy.Isbn);
            }
        }

        return isbns;
    }
}
