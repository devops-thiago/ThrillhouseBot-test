namespace LiftPassValidator;

public interface ILiftStatusFeed
{
    /// <summary>
    /// Returns the current status of a lift. Never returns null.
    /// Throws <see cref="KeyNotFoundException"/> when the lift id is not known to the resort.
    /// </summary>
    LiftStatus Get(string liftId);
}

public sealed class LiftStatusFeed : ILiftStatusFeed
{
    private readonly IReadOnlyDictionary<string, LiftStatus> _lifts;

    public LiftStatusFeed(IEnumerable<LiftStatus> lifts)
    {
        _lifts = lifts.ToDictionary(l => l.LiftId);
    }

    public LiftStatus Get(string liftId) => _lifts[liftId];
}
