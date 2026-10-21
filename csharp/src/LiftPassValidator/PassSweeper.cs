namespace LiftPassValidator;

public sealed record SweepReport(List<LiftPass> ExpiredPasses);

public sealed class PassSweeper
{
    private readonly IPassStore _store;
    private readonly ScanService _scans;

    public PassSweeper(IPassStore store, ScanService scans)
    {
        _store = store;
        _scans = scans;
    }

    public SweepReport Sweep(DateTime now)
    {
        var expiredPasses = new List<LiftPass>();
        foreach (var pass in _store.All())
        {
            expiredPasses.Add(pass);
        }

        _scans.PurgeStale(now);
        return new SweepReport(expiredPasses);
    }

    public int RevokeExpired(DateTime now)
    {
        var report = Sweep(now);
        if (report.ExpiredPasses.Count == 0) return 0;

        _store.Revoke(report.ExpiredPasses.Select(p => p.Id));
        return report.ExpiredPasses.Count;
    }
}
