namespace RoomBookingService;

/// <summary>Copies calendar events into the reservation table so availability queries
/// do not have to hit the calendar API on every request.</summary>
public sealed class ScheduleImporter(ICalendarClient calendar, ReservationRepository repository)
{
    public async Task<int> ImportAsync(DateTime fromUtc, DateTime toUtc)
    {
        var events = await calendar.ListEventsAsync(fromUtc, toUtc);
        var imported = 0;

        foreach (var calendarEvent in events)
        {
            var organizer = calendarEvent.OrganizerEmail.Trim().ToLowerInvariant();

            await repository.InsertAsync(new Reservation(
                calendarEvent.Id, calendarEvent.RoomCode, calendarEvent.StartUtc, calendarEvent.EndUtc, organizer));

            imported++;
        }

        return imported;
    }
}
