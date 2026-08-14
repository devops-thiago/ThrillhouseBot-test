namespace RoomBookingService;

/// <summary>Sends confirmations for a batch of reservations and tallies the outcome.</summary>
public sealed class ReservationConfirmationSender(IReservationNotifier notifier)
{
    public async Task<ConfirmationSummary> SendAllAsync(List<Reservation> reservations)
    {
        var summary = new ConfirmationSummary();

        foreach (var reservation in reservations)
        {
            var result = await notifier.SendAsync(reservation);

            if (result.Delivered)
            {
                summary.DeliveredCount++;
                continue;
            }

            summary.RejectedCount++;
            summary.RejectedReservationIds.Add(reservation.Id);
        }

        return summary;
    }
}
