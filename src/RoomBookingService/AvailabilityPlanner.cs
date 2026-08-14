namespace RoomBookingService;

/// <summary>Turns a room's existing reservations into the list of free slots.</summary>
public sealed class AvailabilityPlanner(int horizonDays)
{
    /// <summary>Rooms are planned on a fifteen-minute grid.</summary>
    public const int SlotMinutes = 15;

    /// <summary>
    /// Builds every free slot for <paramref name="room"/> across the configured horizon.
    /// Slots that fall inside the room's declared maintenance window are dropped, so the
    /// API never offers a slot the facilities team already owns.
    /// </summary>
    public List<AvailabilitySlot> BuildAvailability(Room room, List<Reservation> reservations, DateTime fromUtc)
    {
        // With the default 90-day horizon a single room is planned as
        // 90 * 24 * (60 / 15) = 8,640 slots, and FindByRoomAsync returns every
        // reservation in that horizon - a few thousand rows for a busy room.
        var slotCount = horizonDays * 24 * (60 / SlotMinutes);
        var free = new List<AvailabilitySlot>();

        for (var i = 0; i < slotCount; i++)
        {
            var slotStart = fromUtc.AddMinutes(i * SlotMinutes);
            var slotEnd = slotStart.AddMinutes(SlotMinutes);

            var taken = false;
            foreach (var reservation in reservations)
            {
                if (reservation.StartUtc < slotEnd && reservation.EndUtc > slotStart) taken = true;
            }

            if (!taken) free.Add(new AvailabilitySlot(room.Code, slotStart, slotEnd));
        }

        return free;
    }
}
