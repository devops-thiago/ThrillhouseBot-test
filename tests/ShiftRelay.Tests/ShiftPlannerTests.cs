using Xunit;

namespace ShiftRelay.Tests;

public class ShiftPlannerTests
{
    private static readonly DateTime CycleStart = new(2026, 3, 2, 9, 0, 0, DateTimeKind.Utc);

    private static Rota WeeklyRota(params string[] members) =>
        new("net-core", "Networking", members, CycleStart, TimeSpan.FromDays(7));

    [Fact]
    public void HolderAt_FollowsTheMemberOrder()
    {
        var planner = new ShiftPlanner();
        var rota = WeeklyRota("ana", "bo", "cleo");

        Assert.Equal("ana", planner.HolderAt(rota, [], CycleStart.AddDays(3))!.Engineer);
        Assert.Equal("bo", planner.HolderAt(rota, [], CycleStart.AddDays(8))!.Engineer);
        Assert.Equal("ana", planner.HolderAt(rota, [], CycleStart.AddDays(21))!.Engineer);
    }

    [Fact]
    public void HolderAt_ReturnsNullBeforeTheCycleStarts()
    {
        var planner = new ShiftPlanner();

        Assert.Null(planner.HolderAt(WeeklyRota("ana", "bo"), [], CycleStart.AddHours(-1)));
    }

    [Fact]
    public void HolderAt_PrefersAnAgreedSwap()
    {
        var planner = new ShiftPlanner();
        var rota = WeeklyRota("ana", "bo", "cleo");
        var swap = new ShiftOverride(rota.Code, CycleStart.AddDays(7), "dai", "bo is at the vendor training");

        var holder = planner.HolderAt(rota, [swap], CycleStart.AddDays(9));

        Assert.Equal("dai", holder!.Engineer);
        Assert.Equal(CycleStart.AddDays(7), holder.StartUtc);
    }

    [Fact]
    public void HolderAt_IgnoresASwapForAnotherShift()
    {
        var planner = new ShiftPlanner();
        var rota = WeeklyRota("ana", "bo");
        var swap = new ShiftOverride(rota.Code, CycleStart.AddDays(14), "dai", "cover");

        Assert.Equal("ana", planner.HolderAt(rota, [swap], CycleStart.AddDays(1))!.Engineer);
    }

    [Fact]
    public void BuildSchedule_CoversTheWholeHorizon()
    {
        var planner = new ShiftPlanner();
        var rota = WeeklyRota("ana", "bo", "cleo");

        var schedule = planner.BuildSchedule(rota, [], CycleStart, 28);

        Assert.Equal(4, schedule.Count);
        Assert.Equal(CycleStart, schedule[0].StartUtc);
        Assert.Equal("ana", schedule[3].Engineer);
        Assert.True(schedule[0].Covers(CycleStart.AddDays(2)));
    }

    [Fact]
    public void BuildSchedule_StartsFromTheRunningShift()
    {
        var planner = new ShiftPlanner();
        var rota = WeeklyRota("ana", "bo");

        var schedule = planner.BuildSchedule(rota, [], CycleStart.AddDays(9), 14);

        Assert.Equal(CycleStart.AddDays(7), schedule[0].StartUtc);
        Assert.Equal("bo", schedule[0].Engineer);
    }

    [Fact]
    public void BuildSchedule_IsEmptyForARotaWithNobodyOnIt()
    {
        var planner = new ShiftPlanner();

        Assert.Empty(planner.BuildSchedule(WeeklyRota(), [], CycleStart, 14));
    }
}
