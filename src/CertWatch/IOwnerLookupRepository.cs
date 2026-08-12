namespace CertWatch;

public interface IOwnerLookupRepository
{
    /// <summary>
    /// Looks up the notification email address for the given owner id. Returns
    /// null when no owner record matches the id.
    /// </summary>
    Task<string?> GetOwnerEmailAsync(string ownerId, CancellationToken ct = default);
}
