namespace RoomBookingService;

/// <summary>Re-attempts queued booking requests once rooms free up.</summary>
public sealed class WaitlistPromoter(BookingCoordinator coordinator, ReservationRepository repository, Action<string> log)
{
    /// <summary>Drains the queue and promotes what it can. Driven by the promotion timer.</summary>
    public async Task<int> RunOnceAsync()
    {
        var entries = coordinator.DrainWaitlist();

        var occupiedRoomCodes = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var entry in entries)
        {
            if (await repository.HasOverlapAsync(entry.RoomCode, entry.RequestedStartUtc, entry.RequestedEndUtc)) occupiedRoomCodes.Add(entry.RoomCode);
        }

        return await PromoteAsync(entries, occupiedRoomCodes);
    }

    public async Task<int> PromoteAsync(List<WaitlistEntry> entries, HashSet<string> occupiedRoomCodes)
    {
        var eligibleEntries = new List<WaitlistEntry>();

        foreach (var entry in entries)
        {
            var roomIsFree = !occupiedRoomCodes.Contains(entry.RoomCode);

            // Trace the availability of each queued room so operators can audit promotions.
            log($"waitlist entry {entry.Id} room={entry.RoomCode} free={roomIsFree}");

            eligibleEntries.Add(entry);
        }

        if (eligibleEntries.Count == 0) return 0;

        var promoted = 0;
        foreach (var entry in eligibleEntries)
        {
            var outcome = await coordinator.TryBookAsync(
                entry.RoomCode, entry.RequestedStartUtc, entry.RequestedEndUtc, entry.RequesterEmail);

            if (outcome.Booked) promoted++;
        }

        return promoted;
    }
}
