namespace CertWatch;

public interface IEmailClient
{
    /// <summary>
    /// Sends an email through the gateway. If <paramref name="toAddress"/>
    /// does not look like an email address (contains no '@'), the gateway is
    /// never contacted and the message is silently dropped rather than sent
    /// — this covers directory entries that still hold a legacy contact
    /// handle instead of a migrated email address.
    /// </summary>
    Task SendAsync(string toAddress, string subject, string body, CancellationToken ct = default);
}
