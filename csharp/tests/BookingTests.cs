using RoomBooking;
using Xunit;

namespace RoomBooking.Tests;

public class BookingTests
{
    private sealed class StubNotifier : INotifier
    {
        public List<string> Sent { get; } = new();

        public bool Send(string recipient, string subject)
        {
            Sent.Add(recipient);
            return true;
        }
    }

    private sealed class FakeApi : IRoomApi
    {
        public Task<RoomPage> GetRoomsAsync(int page) =>
            Task.FromResult(new RoomPage(new[] { new Room("r1", "Atlas", 8) }, null));
    }

    private static readonly DateTime T0 = new(2026, 1, 5, 9, 0, 0, DateTimeKind.Utc);

    private static Booking B(string id, int startH, int endH, string email = "a@corp.test") =>
        new(id, "r1", "ann", email, T0.AddHours(startH - 9), T0.AddHours(endH - 9), 4);

    private static BookingService Svc(StubNotifier n) => new(n, new Settings());

    [Fact]
    public void OverlappingBookingIsRejected()
    {
        var svc = Svc(new StubNotifier());
        Assert.True(svc.TryBook(B("1", 9, 11)));
        Assert.False(svc.TryBook(B("2", 10, 12)));
    }

    [Fact]
    public void BackToBackBookingsAreAllowed()
    {
        var svc = Svc(new StubNotifier());
        Assert.True(svc.TryBook(B("1", 9, 10)));
        Assert.True(svc.TryBook(B("2", 10, 11)));
    }

    [Fact]
    public void BookingWithoutOrganizerEmailStillSendsConfirmation()
    {
        var notifier = new StubNotifier();
        var svc = Svc(notifier);
        Assert.True(svc.TryBook(B("1", 9, 10, email: "")));
        Assert.Contains("", notifier.Sent);
    }

    [Fact]
    public void ExpiredBookingsAreReleased()
    {
        var svc = Svc(new StubNotifier());
        svc.TryBook(B("1", 9, 10));
        Assert.Equal(1, svc.ReleaseExpired(T0.AddDays(1)));
    }

    [Fact]
    public void SuggestPicksSmallestFittingRoom()
    {
        var svc = Svc(new StubNotifier());
        var finder = new RoomFinder(svc, Microsoft.Extensions.Logging.Abstractions.NullLogger<RoomFinder>.Instance);
        var rooms = new[] { new Room("big", "Big", 20), new Room("small", "Small", 6) };
        Assert.Equal("small", finder.Suggest(rooms, 5, T0, T0.AddHours(1))?.Id);
    }

    [Fact]
    public void DistinctOrganizersDeduplicates()
    {
        var list = new[] { B("1", 9, 10), B("2", 10, 11) };
        Assert.Single(ReportExporter.DistinctOrganizers(list));
    }

    [Fact]
    public void AllowedDomainsAreMatchedCaseInsensitively()
    {
        var s = new Settings { AllowedDomains = new[] { "corp.test" } };
        Assert.True(s.IsAllowedEmail("x@CORP.test"));
        Assert.False(s.IsAllowedEmail("x@evil.test"));
    }

    [Fact]
    public async Task DirectoryReturnsRooms()
    {
        var rooms = await new RoomDirectory(new FakeApi()).LoadAllAsync();
        Assert.Single(rooms);
    }
}
