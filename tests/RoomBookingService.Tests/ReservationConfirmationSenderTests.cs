using RoomBookingService;
using Xunit;

namespace RoomBookingService.Tests;

public class ReservationConfirmationSenderTests
{
    private sealed class StubReservationNotifier : IReservationNotifier
    {
        public List<string> SentReservationIds { get; } = new();

        public Task<NotificationResult> SendAsync(Reservation reservation)
        {
            SentReservationIds.Add(reservation.Id);
            return Task.FromResult(NotificationResult.Sent());
        }
    }

    private static Reservation MakeReservation(string id, string organizerEmail) =>
        new(id, "B3-1102", new DateTime(2026, 3, 2, 9, 0, 0, DateTimeKind.Utc),
            new DateTime(2026, 3, 2, 10, 0, 0, DateTimeKind.Utc), organizerEmail);

    [Fact]
    public async Task SendAllAsync_DeliversEveryConfirmation()
    {
        var notifier = new StubReservationNotifier();
        var sender = new ReservationConfirmationSender(notifier);

        var summary = await sender.SendAllAsync(new List<Reservation>
        {
            MakeReservation("r-1", "ana@example.com"),
            MakeReservation("r-2", "bo@example.com")
        });

        Assert.Equal(2, summary.DeliveredCount);
        Assert.True(summary.AllDelivered);
    }

    [Fact]
    public async Task SendAllAsync_ReportsRejectionsForUndeliverableAddresses()
    {
        var notifier = new StubReservationNotifier();
        var sender = new ReservationConfirmationSender(notifier);

        var summary = await sender.SendAllAsync(new List<Reservation>
        {
            MakeReservation("r-3", "cleo@example.com"),
            MakeReservation("r-4", "   ")
        });

        Assert.True(summary.AllDelivered);
        Assert.Empty(summary.RejectedReservationIds);
    }
}
