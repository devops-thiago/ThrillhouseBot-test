namespace MuseumTickets;

public sealed record HoldRequest(string Gallery, DateTime Slot, int Quantity, string Email);

public sealed record Hold(Guid Id, string Gallery, DateTime Slot, int Quantity, string Email, DateTime ExpiresAt);

public sealed record PrintRequest(string Gallery, string Template);

public sealed record GroupBooking(string Gallery, DateTime Slot, int Quantity, string Organiser);

public sealed record GroupBookingPage(List<GroupBooking> Items, int Page, int TotalPages, string? NextPage);
