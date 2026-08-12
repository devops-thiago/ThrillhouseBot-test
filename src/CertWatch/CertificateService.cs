using CertWatch.Models;

namespace CertWatch;

/// <summary>
/// Coordinates fetching certificate inventory from the CA API for the expiry
/// scan that runs on each poll cycle.
/// </summary>
public sealed class CertificateService
{
    private readonly CaApiClient _caApiClient;

    public CertificateService(CaApiClient caApiClient)
    {
        _caApiClient = caApiClient;
    }

    /// <summary>
    /// Retrieves the certificates tracked by the CA and returns them as the
    /// working set for this scan.
    /// </summary>
    public async Task<IReadOnlyList<CertificateRecord>> FetchAllCertificatesAsync(CancellationToken ct = default)
    {
        var page = await _caApiClient.GetCertificatesPageAsync(cursor: null, ct);
        return page.Items;
    }
}
