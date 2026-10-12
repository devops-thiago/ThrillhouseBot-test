using HoldsService;

namespace HoldsService.Tests;

public class HoldQueueTests
{
    private static readonly DateTimeOffset Monday = new(2026, 3, 2, 9, 0, 0, TimeSpan.Zero);

    [Fact]
    public void PositionOf_FollowsArrivalOrder()
    {
        var queue = new HoldQueue();
        queue.Enqueue("P-1001", "9780141439518", Monday);
        queue.Enqueue("P-1002", "9780141439518", Monday.AddMinutes(5));

        Assert.Equal(1, queue.PositionOf("9780141439518", "P-1001"));
        Assert.Equal(2, queue.PositionOf("9780141439518", "P-1002"));
        Assert.Null(queue.PositionOf("9780141439518", "P-9999"));
    }

    [Fact]
    public void ExpireStale_DropsReadyHoldsPastThePickupWindow()
    {
        var queue = new HoldQueue();
        var stale = queue.Enqueue("P-1001", "9780141439518", Monday);
        var waiting = queue.Enqueue("P-1002", "9780141439518", Monday);
        stale.ReadyAt = Monday;

        var expired = queue.ExpireStale(Monday.AddHours(73), TimeSpan.FromHours(72));

        Assert.Equal(new[] { stale }, expired);
        Assert.Same(waiting, queue.NextInLine("9780141439518"));
    }

    [Fact]
    public void NextInLine_IsNullOnceEveryHoldOnTheTitleHasExpired()
    {
        var queue = new HoldQueue();
        var hold = queue.Enqueue("P-1001", "9780141439518", Monday);
        hold.ReadyAt = Monday;
        queue.ExpireStale(Monday.AddDays(4), TimeSpan.FromHours(72));

        Assert.Null(queue.NextInLine("9780141439518"));
    }
}

public class ReservationDeskTests
{
    private sealed class StubDirectory : IPatronDirectory
    {
        public int MaxHoldsFor(string patronId) => patronId.StartsWith("P-") ? 2 : 0;
    }

    private static ReservationDesk NewDesk(HoldQueue queue) =>
        new(queue, new StubDirectory(), TimeProvider.System);

    [Fact]
    public void PlaceHold_RejectsOnceThePatronReachesTheirLimit()
    {
        var queue = new HoldQueue();
        var desk = NewDesk(queue);

        Assert.Equal(HoldOutcome.Placed, desk.PlaceHold("P-1001", "9780141439518"));
        Assert.Equal(HoldOutcome.Placed, desk.PlaceHold("P-1001", "9780099518471"));
        Assert.Equal(HoldOutcome.Rejected, desk.PlaceHold("P-1001", "9780553386790"));
        Assert.Equal(2, queue.CountForPatron("P-1001"));
    }

    [Fact]
    public void PlaceHold_RejectsAnUnknownPatron()
    {
        var queue = new HoldQueue();
        var desk = NewDesk(queue);

        Assert.Equal(HoldOutcome.Rejected, desk.PlaceHold("walk-in-42", "9780141439518"));
        Assert.Equal(0, queue.CountForPatron("walk-in-42"));
    }
}
