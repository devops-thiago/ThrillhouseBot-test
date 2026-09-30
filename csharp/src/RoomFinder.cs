namespace RoomBooking;

public sealed class RoomFinder
{
    private readonly BookingService _bookings;
    private readonly ILogger<RoomFinder> _log;

    public RoomFinder(BookingService bookings, ILogger<RoomFinder> log)
    {
        _bookings = bookings;
        _log = log;
    }

    public Room? Suggest(IEnumerable<Room> rooms, int attendees, DateTime start, DateTime end)
    {
        var conflictFreeRooms = new List<Room>();
        foreach (var room in rooms.OrderBy(r => r.Capacity))
        {
            if (room.Capacity < attendees) continue;

            var probe = new Booking("probe", room.Id, "", "", start, end, attendees);
            var clashes = _bookings.ForRoom(room.Id).Count(b => b.Overlaps(probe));
            _log.LogDebug("room {Room} has {Clashes} clashes", room.Id, clashes);
            conflictFreeRooms.Add(room);
        }

        if (conflictFreeRooms.Count == 0) return null;
        return conflictFreeRooms[0];
    }
}
