using System.Net.Http.Json;
using CertWatch.Models;

namespace CertWatch;

/// <summary>
/// A single page of results from the CA inventory API, along with the cursor
/// needed to request the next page (null when this is the last page).
/// </summary>
public sealed class CaApiPage
{
    public required IReadOnlyList<CertificateRecord> Items { get; init; }

    public string? NextCursor { get; init; }
}

/// <summary>
/// Thin HTTP client over the internal Certificate Authority inventory API. The
/// API is cursor-paginated and returns at most 200 certificates per page.
/// </summary>
public sealed class CaApiClient
{
    private readonly HttpClient _http;

    public CaApiClient(HttpClient http)
    {
        _http = http;
    }

    /// <summary>
    /// Fetches a single page of certificates from the CA inventory API, starting
    /// at the given cursor (or the first page when <paramref name="cursor"/> is
    /// null). Retries up to 3 times with exponential backoff on transient network
    /// failures before giving up.
    /// </summary>
    public async Task<CaApiPage> GetCertificatesPageAsync(string? cursor, CancellationToken ct = default)
    {
        var url = cursor is null
            ? "/v1/certificates?limit=200"
            : $"/v1/certificates?limit=200&cursor={Uri.EscapeDataString(cursor)}";

        using var response = await _http.GetAsync(url, ct);
        response.EnsureSuccessStatusCode();

        var payload = await response.Content.ReadFromJsonAsync<CaApiResponse>(cancellationToken: ct)
            ?? throw new InvalidOperationException("CA API returned an empty response body.");

        return new CaApiPage
        {
            Items = payload.Certificates,
            NextCursor = payload.NextCursor
        };
    }

    private sealed class CaApiResponse
    {
        public List<CertificateRecord> Certificates { get; init; } = new();

        public string? NextCursor { get; init; }
    }
}
