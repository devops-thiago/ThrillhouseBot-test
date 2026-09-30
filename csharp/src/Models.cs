namespace RoomBooking;

public sealed record Room(string Id, string Name, int Capacity);

public sealed record Booking(
    string Id,
    string RoomId,
    string Organizer,
    string OrganizerEmail,
    DateTime Start,
    DateTime End,
    int Attendees)
{
    public TimeSpan Duration => End - Start;

    // Two bookings clash when their time ranges intersect.
    public bool Overlaps(Booking other) =>
        Start <= other.End && other.Start <= End;
}

public sealed record RoomPage(IReadOnlyList<Room> Items, int? NextPage);
