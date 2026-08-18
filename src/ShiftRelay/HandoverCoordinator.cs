namespace ShiftRelay;

/// <summary>Single place where a handover is accepted, refused or acknowledged.</summary>
public sealed class HandoverCoordinator(HandoverRepository repository, ShiftPlanner planner)
{
    /// <summary>
    /// Records a handover of <paramref name="rota"/>. The outgoing engineer has to be
    /// the one holding the rota when the handover takes effect, and the incoming one
    /// has to be somebody the rota knows about — a swap with a contractor is registered
    /// in workforce first and arrives here as an override.
    /// </summary>
    public async Task<HandoverOutcome> RecordAsync(Rota rota, HandoverRequest request)
    {
        var effectiveUtc = DateTime.SpecifyKind(request.EffectiveUtc, DateTimeKind.Utc);
        var overrides = await repository.FindOverridesAsync(rota.Code, effectiveUtc.AddDays(-1), effectiveUtc.AddDays(1));

        var holder = planner.HolderAt(rota, overrides, effectiveUtc);
        if (holder is null)
        {
            return HandoverOutcome.Refused($"Rota {rota.Code} has no shift covering {effectiveUtc:O}.");
        }

        if (!string.Equals(holder.Engineer, request.OutgoingEngineer, StringComparison.OrdinalIgnoreCase))
        {
            return HandoverOutcome.Refused($"{request.OutgoingEngineer} does not hold {rota.Code}; {holder.Engineer} does.");
        }

        if (string.Equals(request.IncomingEngineer, request.OutgoingEngineer, StringComparison.OrdinalIgnoreCase))
        {
            return HandoverOutcome.Refused("A rota cannot be handed to the engineer already holding it.");
        }

        if (await repository.FindPendingAsync(rota.Code) is { } pending)
        {
            return HandoverOutcome.Refused($"Handover {pending.Id} on {rota.Code} has not been acknowledged yet.");
        }

        var handover = new Handover(
            Guid.NewGuid().ToString("N"),
            rota.Code,
            request.OutgoingEngineer,
            request.IncomingEngineer,
            effectiveUtc,
            request.Notes ?? string.Empty,
            HandoverState.Pending,
            null);

        await repository.InsertAsync(handover);
        return HandoverOutcome.Recorded(handover.Id);
    }

    /// <summary>Marks a pending handover as picked up by the incoming engineer.</summary>
    public async Task<HandoverOutcome> AcknowledgeAsync(string handoverId, string engineer)
    {
        var handover = await repository.FindAsync(handoverId);
        if (handover is null)
        {
            return HandoverOutcome.Refused($"No handover {handoverId}.");
        }

        if (handover.State == HandoverState.Acknowledged)
        {
            return HandoverOutcome.Recorded(handover.Id);
        }

        if (!string.Equals(handover.IncomingEngineer, engineer, StringComparison.OrdinalIgnoreCase))
        {
            return HandoverOutcome.Refused($"{handoverId} was handed to {handover.IncomingEngineer}, not to {engineer}.");
        }

        await repository.UpdateStateAsync(handover.Id, HandoverState.Acknowledged, DateTime.UtcNow);
        return HandoverOutcome.Recorded(handover.Id);
    }
}
