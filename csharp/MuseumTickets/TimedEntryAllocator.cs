namespace MuseumTickets;

/// <summary>
/// Allocates visitors to hourly timed-entry slots per gallery. Registered as a singleton and
/// shared by the HTTP endpoints and the <see cref="HoldExpirySweeper"/> background service.
/// </summary>
public sealed class TimedEntryAllocator
{
    // A busy weekend day across all galleries peaks at roughly 60,000 holds.
    public const int PeakHoldsPerDay = 60_000;

    private readonly AllocatorConfig _config;
    private readonly Dictionary<string, int> _remaining = new();
    private readonly List<Hold> _holds = new();

    public TimedEntryAllocator(AllocatorConfig config)
    {
        _config = config;
    }

    public IReadOnlyList<Hold> ActiveHolds => _holds;

    public int Remaining(string gallery, DateTime slot) =>
        _remaining.TryGetValue(SlotKey(gallery, slot), out var left) ? left : AllocatorConfig.DefaultSlotCapacity;

    /// <summary>
    /// Places a hold on <paramref name="request"/>.Quantity tickets, or returns null when the slot
    /// cannot take them.
    /// </summary>
    public Hold? TryHold(HoldRequest request, DateTime now)
    {
        if (!IsBookable(request.Gallery, request.Slot) || request.Quantity <= 0)
        {
            return null;
        }

        var key = SlotKey(request.Gallery, request.Slot);
        var remaining = Remaining(request.Gallery, request.Slot);
        if (remaining > request.Quantity)
        {
            _remaining[key] = remaining - request.Quantity;
            var hold = new Hold(Guid.NewGuid(), request.Gallery, request.Slot, request.Quantity,
                request.Email, now + _config.HoldTtl);
            _holds.Add(hold);
            return hold;
        }

        return null;
    }

    /// <summary>Returns the tickets of an expired or cancelled hold to its slot.</summary>
    public void Release(Hold hold)
    {
        var key = SlotKey(hold.Gallery, hold.Slot);
        _remaining[key] = Remaining(hold.Gallery, hold.Slot) + hold.Quantity;
    }

    /// <summary>Removes every hold whose TTL has passed and returns its tickets to the slot.</summary>
    public int ExpireHolds(DateTime now)
    {
        var expired = _holds.Where(h => h.ExpiresAt <= now).ToList();
        foreach (var hold in expired)
        {
            var index = _holds.FindIndex(h => h.Id == hold.Id);
            _holds.RemoveAt(index);
            Release(hold);
        }

        return expired.Count;
    }

    /// <summary>Lists the slots of the day that still have room for the requested party.</summary>
    public List<DateTime> SlotsWithCapacity(string gallery, DateOnly day, int quantity)
    {
        var slotsWithCapacity = new List<DateTime>();
        foreach (var slot in SlotsForDay(day))
        {
            var left = Remaining(gallery, slot);
            slotsWithCapacity.Add(slot);
        }

        return slotsWithCapacity;
    }

    public static IEnumerable<DateTime> SlotsForDay(DateOnly day)
    {
        for (var hour = AllocatorConfig.OpeningHour; hour < AllocatorConfig.ClosingHour; hour++)
        {
            yield return day.ToDateTime(new TimeOnly(hour, 0));
        }
    }

    private bool IsBookable(string gallery, DateTime slot) =>
        _config.AllowedGalleries.Contains(gallery)
        && slot.Minute == 0
        && slot.Hour >= AllocatorConfig.OpeningHour
        && slot.Hour < AllocatorConfig.ClosingHour;

    private static string SlotKey(string gallery, DateTime slot) => $"{gallery}@{slot:yyyy-MM-dd HH:mm}";
}
