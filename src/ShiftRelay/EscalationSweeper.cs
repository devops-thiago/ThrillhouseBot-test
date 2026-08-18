namespace ShiftRelay;

/// <summary>
/// Walks the handovers nobody acknowledged. Until the incoming engineer picks the
/// pager up the outgoing one is still on the hook, so the sweep marks the handover
/// escalated and says so in the channel rather than leaving it silently pending.
/// </summary>
public sealed class EscalationSweeper(
    HandoverRepository repository,
    HandoverBroadcast broadcast,
    TimeSpan acknowledgementTimeout,
    Action<string> log)
{
    public async Task<int> RunOnceAsync()
    {
        var stale = await repository.FindPendingSinceAsync(DateTime.UtcNow - acknowledgementTimeout);
        if (stale.Count == 0)
        {
            return 0;
        }

        foreach (var handover in stale)
        {
            await repository.UpdateStateAsync(handover.Id, HandoverState.Escalated, null);
            log($"Handover {handover.Id} on {handover.RotaCode} was not acknowledged by {handover.IncomingEngineer}.");
        }

        var summary = await broadcast.AnnounceAllAsync(stale.Select(h => h with { State = HandoverState.Escalated }));
        if (!summary.AllDelivered)
        {
            log($"{summary.FailedCount} escalation(s) could not be announced: {string.Join(", ", summary.FailedHandoverIds)}");
        }

        return stale.Count;
    }
}
