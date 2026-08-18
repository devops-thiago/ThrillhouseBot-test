using Xunit;

namespace ShiftRelay.Tests;

public class HandoverBroadcastTests
{
    private sealed class StubAnnouncer(params string[] failingHandoverIds) : IHandoverAnnouncer
    {
        public List<string> AnnouncedIds { get; } = new();

        public Task<AnnouncementResult> AnnounceAsync(Handover handover)
        {
            AnnouncedIds.Add(handover.Id);
            return Task.FromResult(failingHandoverIds.Contains(handover.Id)
                ? AnnouncementResult.Failed("Chat gateway answered 503.")
                : AnnouncementResult.Sent());
        }
    }

    private sealed class ThrowingAnnouncer : IHandoverAnnouncer
    {
        public Task<AnnouncementResult> AnnounceAsync(Handover handover) =>
            throw new HttpRequestException("Name or service not known.");
    }

    private static Handover MakeHandover(string id) => new(
        id,
        "net-core",
        "ana",
        "bo",
        new DateTime(2026, 3, 9, 9, 0, 0, DateTimeKind.Utc),
        "cert renewal still open",
        HandoverState.Pending,
        null);

    [Fact]
    public async Task AnnounceAllAsync_DeliversEveryHandover()
    {
        var announcer = new StubAnnouncer();
        var broadcast = new HandoverBroadcast(announcer);

        var summary = await broadcast.AnnounceAllAsync([MakeHandover("h-1"), MakeHandover("h-2")]);

        Assert.Equal(2, summary.DeliveredCount);
        Assert.True(summary.AllDelivered);
        Assert.Equal(["h-1", "h-2"], announcer.AnnouncedIds);
    }

    [Fact]
    public async Task AnnounceAllAsync_KeepsGoingAfterARejectedDelivery()
    {
        var announcer = new StubAnnouncer("h-1");
        var broadcast = new HandoverBroadcast(announcer);

        var summary = await broadcast.AnnounceAllAsync([MakeHandover("h-1"), MakeHandover("h-2")]);

        Assert.Equal(1, summary.DeliveredCount);
        Assert.Equal(1, summary.FailedCount);
        Assert.Equal(["h-1"], summary.FailedHandoverIds);
        Assert.False(summary.AllDelivered);
    }

    [Fact]
    public async Task AnnounceAllAsync_TreatsAGatewayOutageAsAFailedDelivery()
    {
        var broadcast = new HandoverBroadcast(new ThrowingAnnouncer());

        var summary = await broadcast.AnnounceAllAsync([MakeHandover("h-3")]);

        Assert.Equal(1, summary.FailedCount);
        Assert.Equal(["h-3"], summary.FailedHandoverIds);
    }
}
