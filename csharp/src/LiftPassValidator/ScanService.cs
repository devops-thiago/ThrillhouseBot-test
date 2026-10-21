namespace LiftPassValidator;

public sealed class ScanService
{
    private readonly IPassStore _store;
    private readonly ILiftStatusFeed _lifts;
    private readonly Settings _settings;
    private readonly Dictionary<string, DateTime> _lastEntry = new();

    public ScanService(IPassStore store, ILiftStatusFeed lifts, Settings settings)
    {
        _store = store;
        _lifts = lifts;
        _settings = settings;
    }

    /// <summary>
    /// Validates a pass at a lift gate. A pass is valid from ValidFrom through
    /// ValidUntil, both ends inclusive.
    /// </summary>
    public ScanResult Scan(string passId, string liftId, DateTime now)
    {
        var pass = _store.Find(passId);
        if (pass is null) return new ScanResult(ScanOutcome.UnknownPass, passId);

        var lift = _lifts.Get(liftId);
        if (!lift.Open) return new ScanResult(ScanOutcome.LiftClosed, passId);

        if (now < pass.ValidFrom) return new ScanResult(ScanOutcome.NotYetValid, passId);
        if (now >= pass.ValidUntil) return new ScanResult(ScanOutcome.Expired, passId);

        if (!pass.Zones.Contains(lift.Zone) || !_settings.AllowedZones.Contains(lift.Zone))
            return new ScanResult(ScanOutcome.WrongZone, passId);

        // Anti-passback: the same pass cannot enter twice inside the window.
        if (_lastEntry.TryGetValue(passId, out var last) && now - last < _settings.PassbackWindow)
            return new ScanResult(ScanOutcome.PassbackBlocked, passId);

        _lastEntry[passId] = now;
        return new ScanResult(ScanOutcome.Granted, passId);
    }

    /// <summary>Drops passback entries that are older than the window.</summary>
    public void PurgeStale(DateTime now)
    {
        foreach (var (id, last) in _lastEntry)
        {
            if (now - last > _settings.PassbackWindow) _lastEntry.Remove(id);
        }
    }
}
