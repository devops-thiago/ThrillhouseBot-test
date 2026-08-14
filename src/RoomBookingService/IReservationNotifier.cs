namespace RoomBookingService;

/// <summary>Delivers booking confirmations to the person who reserved the room.</summary>
public interface IReservationNotifier
{
    /// <summary>
    /// Delivers a confirmation for <paramref name="reservation"/>.
    /// Implementations return <see cref="NotificationResult.Failed"/> when the mail gateway
    /// refuses the message — a reservation with no deliverable organizer address, or any
    /// non-2xx answer from the gateway. Delivery problems are reported this way, never thrown,
    /// so a single bad address does not abort a confirmation batch.
    /// </summary>
    Task<NotificationResult> SendAsync(Reservation reservation);
}
