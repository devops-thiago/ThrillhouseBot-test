namespace ShiftRelay;

/// <summary>
/// Announces a batch of handovers and reports what got through. One failed delivery
/// does not stop the rest of the batch: a chat outage should not hide the handovers
/// that happened during it.
/// </summary>
public sealed class HandoverBroadcast(IHandoverAnnouncer announcer)
{
    public async Task<AnnouncementSummary> AnnounceAllAsync(IEnumerable<Handover> handovers)
    {
        var summary = new AnnouncementSummary();

        foreach (var handover in handovers)
        {
            AnnouncementResult result;
            try
            {
                result = await announcer.AnnounceAsync(handover);
            }
            catch (HttpRequestException exception)
            {
                result = AnnouncementResult.Failed(exception.Message);
            }

            if (result.Delivered)
            {
                summary.DeliveredCount++;
            }
            else
            {
                summary.FailedCount++;
                summary.FailedHandoverIds.Add(handover.Id);
            }
        }

        return summary;
    }
}
