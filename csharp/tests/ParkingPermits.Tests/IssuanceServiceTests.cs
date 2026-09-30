using ParkingPermits;
using Xunit;

namespace ParkingPermits.Tests;

public class IssuanceServiceTests
{
    private static readonly DateTime Now = new(2026, 1, 15, 9, 0, 0, DateTimeKind.Utc);

    private class StubRegistry : IRegistryClient
    {
        public Task<VehiclePage> GetVehiclesAsync(string address, string? cursor) =>
            Task.FromResult(new VehiclePage(new[] { new Vehicle("ABC1234", address) }, null));
    }

    private class StubGateway : IFeeGateway
    {
        public bool Decline;

        public ChargeResult Charge(string applicantEmail, decimal amount) =>
            Decline ? new ChargeResult(false, null) : new ChargeResult(true, "ch_test");
    }

    private static (IssuanceService Service, InMemoryPermitStore Store, StubGateway Gateway) Build(int max = 2)
    {
        var store = new InMemoryPermitStore();
        var gateway = new StubGateway();
        var settings = new PermitSettings
        {
            AllowedZones = new HashSet<string> { "A", "B" },
            MaxPerAddress = max,
        };
        var service = new IssuanceService(store, new StubRegistry(), gateway, new ZoneQuota(500), settings, () => Now);
        return (service, store, gateway);
    }

    private static PermitApplication Application(string zone = "A") =>
        new("ABC1234", "12 Elm Street", zone, "resident@example.com");

    [Fact]
    public async Task Issue_ForRegisteredVehicle_CreatesActivePermit()
    {
        var (service, store, _) = Build();
        var outcome = await service.IssueAsync(Application());
        Assert.True(outcome.Issued);
        Assert.Equal(Now.AddDays(365), outcome.Permit!.ExpiresAt);
        Assert.Equal(1, store.CountForAddress("12 Elm Street"));
    }

    [Fact]
    public async Task Issue_RejectsZoneOutsideAllowedList()
    {
        var (service, _, _) = Build();
        var outcome = await service.IssueAsync(Application("Z"));
        Assert.False(outcome.Issued);
    }

    [Fact]
    public async Task Issue_RejectsWhenAddressAlreadyAtLimit()
    {
        var (service, store, _) = Build(max: 2);
        for (var i = 0; i < 2; i++)
        {
            store.Save(new Permit($"PRM-{i}", $"OLD{i}", "12 Elm Street", "A", Now, Now.AddDays(30), PermitStatus.Active));
        }

        var outcome = await service.IssueAsync(Application());
        Assert.False(outcome.Issued);
        Assert.Equal(2, store.CountForAddress("12 Elm Street"));
    }

    [Fact]
    public async Task Issue_WhenCardDeclined_ReturnsDeclinedOutcome()
    {
        var (service, store, gateway) = Build();
        gateway.Decline = true;
        var outcome = await service.IssueAsync(Application());
        Assert.False(outcome.Issued);
        Assert.Equal("Payment declined", outcome.Message);
        Assert.Equal(0, store.CountForAddress("12 Elm Street"));
    }

    [Fact]
    public async Task IssueBatch_CollectsIssuedPermitsAndErrors()
    {
        var (service, _, _) = Build();
        var result = await service.IssueBatchAsync(new[] { Application("A"), Application("Z") });
        Assert.Single(result.Issued);
        Assert.Contains(result.Errors, e => e.Contains("zone Z"));
    }

    [Fact]
    public void FindDuplicatePlates_ReportsPlatesWithTwoActivePermits()
    {
        var permits = new[]
        {
            new Permit("1", "AAA", "x", "A", Now, Now.AddDays(1), PermitStatus.Active),
            new Permit("2", "AAA", "y", "A", Now, Now.AddDays(1), PermitStatus.Active),
            new Permit("3", "BBB", "z", "A", Now, Now.AddDays(1), PermitStatus.Active),
        };
        Assert.Equal(new[] { "AAA" }, PermitReports.FindDuplicatePlates(permits));
    }

    [Fact]
    public void ZoneQuota_StopsReservingAtCapacity()
    {
        var quota = new ZoneQuota(1);
        Assert.True(quota.TryReserve("A"));
        Assert.False(quota.TryReserve("A"));
        quota.Release("A");
        Assert.True(quota.TryReserve("A"));
    }
}
