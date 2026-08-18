namespace ShiftRelay;

/// <summary>
/// Tells people a handover happened. Split out from the chat implementation so the
/// batch logic can be tested without a gateway, and so a second channel (the paging
/// provider) can be added without touching the coordinator.
/// </summary>
public interface IHandoverAnnouncer
{
    Task<AnnouncementResult> AnnounceAsync(Handover handover);
}
