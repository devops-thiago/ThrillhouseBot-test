namespace ShiftRelay;

/// <summary>
/// Turns a rota into concrete shifts. The rotation itself is fixed — member
/// <c>n</c> of the order takes shift <c>n</c> of the cycle — and swaps are applied
/// on top of it, so the published schedule and the agreed cover never disagree.
/// </summary>
public sealed class ShiftPlanner
{
    /// <summary>The shift running at <paramref name="instantUtc"/>, or null when the rota has not started yet.</summary>
    public ShiftAssignment? HolderAt(Rota rota, IReadOnlyList<ShiftOverride> overrides, DateTime instantUtc)
    {
        if (rota.Members.Count == 0 || rota.ShiftLength <= TimeSpan.Zero || instantUtc < rota.CycleStartUtc)
        {
            return null;
        }

        var elapsed = instantUtc - rota.CycleStartUtc;
        var index = (long)(elapsed.Ticks / rota.ShiftLength.Ticks);
        return Assign(rota, overrides, index);
    }

    /// <summary>
    /// Every shift that starts inside the window, beginning with the one running at
    /// <paramref name="fromUtc"/>. This is what the schedule endpoint returns and
    /// what the weekly on-call mail is rendered from.
    /// </summary>
    public IReadOnlyList<ShiftAssignment> BuildSchedule(Rota rota, IReadOnlyList<ShiftOverride> overrides, DateTime fromUtc, int horizonDays)
    {
        var schedule = new List<ShiftAssignment>();
        var running = HolderAt(rota, overrides, fromUtc);
        if (running is null)
        {
            return schedule;
        }

        var untilUtc = fromUtc.AddDays(horizonDays);
        var index = (long)((running.StartUtc - rota.CycleStartUtc).Ticks / rota.ShiftLength.Ticks);
        for (var shift = Assign(rota, overrides, index); shift.StartUtc < untilUtc; shift = Assign(rota, overrides, ++index))
        {
            schedule.Add(shift);
        }

        return schedule;
    }

    private static ShiftAssignment Assign(Rota rota, IReadOnlyList<ShiftOverride> overrides, long index)
    {
        var startUtc = rota.CycleStartUtc + TimeSpan.FromTicks(rota.ShiftLength.Ticks * index);
        var scheduled = rota.Members[(int)(index % rota.Members.Count)];
        var swap = overrides.FirstOrDefault(o => o.RotaCode == rota.Code && o.ShiftStartUtc == startUtc);

        return new ShiftAssignment(rota.Code, swap?.Engineer ?? scheduled, startUtc, startUtc + rota.ShiftLength);
    }
}
