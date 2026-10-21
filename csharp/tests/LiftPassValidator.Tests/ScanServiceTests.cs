using LiftPassValidator;
using Xunit;

namespace LiftPassValidator.Tests;

public class ScanServiceTests
{
    private static readonly DateTime Noon = new(2026, 1, 15, 12, 0, 0, DateTimeKind.Utc);

    private sealed class FakeStore : IPassStore
    {
        private readonly List<LiftPass> _passes;
        public FakeStore(params LiftPass[] passes) => _passes = passes.ToList();
        public LiftPass? Find(string passId) => _passes.FirstOrDefault(p => p.Id == passId);
        public IReadOnlyList<LiftPass> FindByHolder(string holder, string zone) =>
            _passes.Where(p => p.Holder == holder && p.Zones.Contains(zone)).ToList();
        public IReadOnlyList<LiftPass> All() => _passes;
        public void Revoke(IEnumerable<string> passIds) =>
            _passes.RemoveAll(p => passIds.Contains(p.Id));
    }

    private sealed class StubLifts : ILiftStatusFeed
    {
        public LiftStatus Get(string liftId) => liftId == "lift-closed"
            ? new LiftStatus(liftId, false, "valley")
            : new LiftStatus(liftId, true, "valley");
    }

    private static Settings TestSettings() => new(
        "test.db", new[] { "valley", "summit" }, TimeSpan.FromMinutes(10), "http://localhost");

    private static LiftPass Pass(string id, DateTime until) =>
        new(id, "alice", PassKind.Day, Noon.AddHours(-4), until, new[] { "valley" });

    private static ScanService Service(params LiftPass[] passes) =>
        new(new FakeStore(passes), new StubLifts(), TestSettings());

    [Fact]
    public void Scan_ValidPass_IsGranted()
    {
        var svc = Service(Pass("p1", Noon.AddHours(4)));
        Assert.Equal(ScanOutcome.Granted, svc.Scan("p1", "lift-1", Noon).Outcome);
    }

    [Fact]
    public void Scan_AtExactValidUntil_IsGranted()
    {
        var svc = Service(Pass("p1", Noon));
        Assert.Equal(ScanOutcome.Granted, svc.Scan("p1", "lift-1", Noon).Outcome);
    }

    [Fact]
    public void Scan_AfterValidUntil_IsExpired()
    {
        var svc = Service(Pass("p1", Noon.AddMinutes(-1)));
        Assert.Equal(ScanOutcome.Expired, svc.Scan("p1", "lift-1", Noon).Outcome);
    }

    [Fact]
    public void Scan_UnknownPass_IsRejected()
    {
        var svc = Service();
        Assert.Equal(ScanOutcome.UnknownPass, svc.Scan("nope", "lift-1", Noon).Outcome);
    }

    [Fact]
    public void Scan_ClosedLift_IsDenied()
    {
        var svc = Service(Pass("p1", Noon.AddHours(4)));
        Assert.Equal(ScanOutcome.LiftClosed, svc.Scan("p1", "lift-closed", Noon).Outcome);
    }

    [Fact]
    public void Scan_AtUnknownLift_IsGranted()
    {
        var svc = Service(Pass("p1", Noon.AddHours(4)));
        Assert.Equal(ScanOutcome.Granted, svc.Scan("p1", "lift-does-not-exist", Noon).Outcome);
    }

    [Fact]
    public void Scan_SecondEntryInsideWindow_IsBlocked()
    {
        var svc = Service(Pass("p1", Noon.AddHours(4)));
        svc.Scan("p1", "lift-1", Noon);
        Assert.Equal(ScanOutcome.PassbackBlocked, svc.Scan("p1", "lift-1", Noon.AddMinutes(2)).Outcome);
    }

    [Fact]
    public void Scan_SecondEntryAfterWindow_IsGranted()
    {
        var svc = Service(Pass("p1", Noon.AddHours(4)));
        svc.Scan("p1", "lift-1", Noon);
        Assert.Equal(ScanOutcome.Granted, svc.Scan("p1", "lift-1", Noon.AddMinutes(11)).Outcome);
    }

    [Fact]
    public void Dedupe_KeepsFirstOccurrence()
    {
        var path = Path.GetTempFileName();
        File.WriteAllLines(path, new[] { "a", "b", "a", "c", "b" });
        Assert.Equal(new[] { "a", "b", "c" }, ScanLog.Dedupe(path));
        File.Delete(path);
    }
}
