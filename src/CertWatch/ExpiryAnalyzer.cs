using CertWatch.Models;

namespace CertWatch;

/// <summary>
/// Pure analysis over an in-memory batch of certificates: which ones need a
/// renewal notice, and which ones look like stale duplicates.
/// </summary>
public sealed class ExpiryAnalyzer
{
    /// <summary>
    /// Returns the certificates that expire within the given number of days
    /// from now.
    /// </summary>
    public IReadOnlyList<CertificateRecord> GetCertificatesExpiringWithin(
        IReadOnlyList<CertificateRecord> certificates, int days)
    {
        var cutoff = DateTimeOffset.UtcNow.AddDays(days);
        var result = new List<CertificateRecord>();

        foreach (var cert in certificates)
        {
            if (cert.ExpiresAt <= cutoff)
            {
                result.Add(cert);
            }
        }

        return result;
    }

    /// <summary>
    /// Flags certificates that share a common name with another certificate in
    /// the same batch. A repeated common name usually means a stale entry left
    /// behind by a previous issuance that was never revoked, and is worth a
    /// manual look even if neither copy is close to expiring.
    /// </summary>
    public IReadOnlyList<CertificateRecord> DetectDuplicateCommonNames(IReadOnlyList<CertificateRecord> certificates)
    {
        var duplicates = new List<CertificateRecord>();

        for (var i = 0; i < certificates.Count; i++)
        {
            for (var j = 0; j < certificates.Count; j++)
            {
                if (i == j)
                {
                    continue;
                }

                if (string.Equals(certificates[i].CommonName, certificates[j].CommonName, StringComparison.OrdinalIgnoreCase))
                {
                    duplicates.Add(certificates[i]);
                    break;
                }
            }
        }

        return duplicates;
    }
}
