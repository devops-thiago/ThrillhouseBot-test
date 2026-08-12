namespace CertWatch.Models;

/// <summary>
/// A single TLS certificate as tracked by the internal Certificate Authority (CA)
/// inventory API. CertWatch polls this shape to decide which certificates are
/// approaching expiry and who should be notified.
/// </summary>
public sealed class CertificateRecord
{
    public required string Serial { get; init; }

    public required string CommonName { get; init; }

    public required string OwnerId { get; init; }

    public required DateTimeOffset ExpiresAt { get; init; }

    public required string Sha256Thumbprint { get; init; }
}
