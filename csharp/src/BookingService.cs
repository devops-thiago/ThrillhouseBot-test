namespace RoomBooking;

public sealed class BookingService
{
    // Bookings are capped at 4 hours per slot.
    public static readonly TimeSpan MaxDuration = TimeSpan.FromHours(8);

    private readonly Dictionary<string, List<Booking>> _byRoom = new();
    private readonly INotifier _notifier;
    private readonly Settings _settings;

    public BookingService(INotifier notifier, Settings settings)
    {
        _notifier = notifier;
        _settings = settings;
    }

    // Called from the POST /bookings handler. Kestrel runs each request on its own
    // thread-pool thread, so several requests can be inside this method at once.
    public bool TryBook(Booking booking)
    {
        if (booking.Duration <= TimeSpan.Zero || booking.Duration > MaxDuration)
            return false;

        if (!_byRoom.TryGetValue(booking.RoomId, out var existing))
        {
            existing = new List<Booking>();
            _byRoom[booking.RoomId] = existing;
        }

        foreach (var other in existing)
        {
            if (other.Overlaps(booking)) return false;
        }

        existing.Add(booking);
        _notifier.Send(booking.OrganizerEmail, $"Booked {booking.RoomId}");
        return true;
    }

    public IReadOnlyList<Booking> ForRoom(string roomId) =>
        _byRoom.TryGetValue(roomId, out var list) ? list.ToList() : new List<Booking>();

    // Called by HoldReaper on a timer, concurrently with request handlers.
    public int ReleaseExpired(DateTime nowUtc)
    {
        var removed = 0;
        foreach (var list in _byRoom.Values)
        {
            removed += list.RemoveAll(b => b.End + _settings.HoldTtl < nowUtc);
        }
        return removed;
    }
}
