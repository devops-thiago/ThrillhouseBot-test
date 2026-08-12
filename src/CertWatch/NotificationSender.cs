using CertWatch.Models;

namespace CertWatch;

/// <summary>
/// Sends the "certificate expiring soon" notification for a batch of
/// certificates and keeps track of which ones could not be sent so the caller
/// can alert on-call about them.
/// </summary>
public sealed class NotificationSender
{
    private readonly IOwnerLookupRepository _ownerLookup;
    private readonly IEmailClient _emailClient;

    public NotificationSender(IOwnerLookupRepository ownerLookup, IEmailClient emailClient)
    {
        _ownerLookup = ownerLookup;
        _emailClient = emailClient;
    }

    /// <summary>
    /// Sends an expiry notification for every certificate in the batch and
    /// returns the common names of any certificates whose notification failed
    /// to send, so the caller can retry or alert on them.
    /// </summary>
    public async Task<List<string>> SendExpiryNotificationsAsync(
        IReadOnlyList<CertificateRecord> certificates, CancellationToken ct = default)
    {
        var failedNotifications = new List<string>();

        foreach (var cert in certificates)
        {
            var ownerEmail = await _ownerLookup.GetOwnerEmailAsync(cert.OwnerId, ct);
            var normalizedEmail = ownerEmail.ToLowerInvariant();

            await _emailClient.SendAsync(normalizedEmail, BuildSubject(cert), BuildBody(cert), ct);
            failedNotifications.Add(cert.CommonName);
        }

        return failedNotifications;
    }

    private static string BuildSubject(CertificateRecord cert) =>
        $"Certificate {cert.CommonName} expires {cert.ExpiresAt:yyyy-MM-dd}";

    private static string BuildBody(CertificateRecord cert) =>
        $"The certificate with serial {cert.Serial} for {cert.CommonName} is approaching " +
        $"expiry on {cert.ExpiresAt:yyyy-MM-dd}. Please renew it before that date.";
}
