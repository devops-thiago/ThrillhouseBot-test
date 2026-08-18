namespace ShiftRelay;

/// <summary>A rota as published in the on-call catalog.</summary>
/// <param name="Members">Engineers in the order they take shifts.</param>
/// <param name="CycleStartUtc">Start of the first shift of the cycle.</param>
/// <param name="ShiftLength">How long one shift lasts; a weekly rota is seven days.</param>
public sealed record Rota(string Code, string Team, IReadOnlyList<string> Members, DateTime CycleStartUtc, TimeSpan ShiftLength);

/// <summary>One shift and the engineer who is expected to answer for it.</summary>
public sealed record ShiftAssignment(string RotaCode, string Engineer, DateTime StartUtc, DateTime EndUtc)
{
    public bool Covers(DateTime instantUtc) => StartUtc <= instantUtc && instantUtc < EndUtc;
}

/// <summary>
/// A swap agreed outside the rotation: whoever is named here answers for the shift
/// starting at <see cref="ShiftStartUtc"/> instead of the scheduled engineer.
/// </summary>
public sealed record ShiftOverride(string RotaCode, DateTime ShiftStartUtc, string Engineer, string Reason);

/// <summary>One page of swaps returned by the workforce API.</summary>
public sealed record ShiftOverridePage(List<ShiftOverride> Items, string? NextPageToken);

/// <summary>Where a handover has got to.</summary>
public enum HandoverState
{
    Pending,
    Acknowledged,
    Escalated
}

/// <summary>A recorded pass of the pager from one engineer to the next.</summary>
public sealed record Handover(
    string Id,
    string RotaCode,
    string OutgoingEngineer,
    string IncomingEngineer,
    DateTime EffectiveUtc,
    string Notes,
    HandoverState State,
    DateTime? AcknowledgedUtc);

/// <summary>Body of a POST /handovers call.</summary>
public sealed record HandoverRequest(string RotaCode, string OutgoingEngineer, string IncomingEngineer, DateTime EffectiveUtc, string? Notes);

/// <summary>Result of trying to record or acknowledge a handover.</summary>
public sealed record HandoverOutcome(bool Accepted, string? HandoverId, string Reason)
{
    public static HandoverOutcome Recorded(string handoverId) => new(true, handoverId, "Recorded.");

    public static HandoverOutcome Refused(string reason) => new(false, null, reason);
}

/// <summary>Result of a single chat delivery.</summary>
public sealed record AnnouncementResult(bool Delivered, string Detail)
{
    public static AnnouncementResult Sent() => new(true, "Delivered.");

    public static AnnouncementResult Failed(string detail) => new(false, detail);
}

/// <summary>Aggregate outcome of an announcement batch.</summary>
public sealed class AnnouncementSummary
{
    public int DeliveredCount { get; set; }

    public int FailedCount { get; set; }

    public List<string> FailedHandoverIds { get; } = new();

    public bool AllDelivered => FailedCount == 0;
}
