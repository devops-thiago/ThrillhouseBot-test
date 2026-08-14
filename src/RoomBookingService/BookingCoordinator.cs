namespace RoomBookingService;

/// <summary>Single place where a room actually gets booked.</summary>
public sealed class BookingCoordinator(ReservationRepository repository, int holdTtlSeconds)
{
    // A slot is held for a short while after the availability check so the person
    // filling in the booking form does not lose the room to someone else browsing
    // the same availability page.
    private readonly Dictionary<string, DateTime> _activeHolds = new();

    // Requests that lost the room are queued here for the promotion timer to retry.
    private readonly List<WaitlistEntry> _waitlist = new();

    public async Task<BookingOutcome> TryBookAsync(string roomCode, DateTime startUtc, DateTime endUtc, string organizerEmail)
    {
        var holdKey = $"{roomCode}|{startUtc:O}";

        if (_activeHolds.TryGetValue(holdKey, out var heldUntil))
        {
            if (heldUntil > DateTime.UtcNow) return BookingOutcome.Rejected("Slot is on hold for another requester.");

            _activeHolds.Remove(holdKey);
        }

        if (await repository.HasOverlapAsync(roomCode, startUtc, endUtc))
        {
            _waitlist.Add(new WaitlistEntry(Guid.NewGuid().ToString("N"), roomCode, startUtc, endUtc, organizerEmail));
            return BookingOutcome.Rejected("Room is already booked for that window.");
        }

        _activeHolds[holdKey] = DateTime.UtcNow.AddSeconds(holdTtlSeconds);

        var reservation = new Reservation(Guid.NewGuid().ToString("N"), roomCode, startUtc, endUtc, organizerEmail);
        await repository.InsertAsync(reservation);

        _activeHolds.Remove(holdKey);
        return BookingOutcome.Confirmed(reservation.Id);
    }

    /// <summary>Takes everything currently queued and empties the waitlist.</summary>
    public List<WaitlistEntry> DrainWaitlist()
    {
        var queued = new List<WaitlistEntry>(_waitlist);
        _waitlist.Clear();
        return queued;
    }
}
