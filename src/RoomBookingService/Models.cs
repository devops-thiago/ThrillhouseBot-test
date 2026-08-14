namespace RoomBookingService;

/// <summary>A bookable meeting room, as published in the facilities room catalog.</summary>
/// <param name="MaintenanceStartUtc">Start of the weekly maintenance window, as a UTC time of day.</param>
/// <param name="MaintenanceDuration">How long the weekly maintenance window lasts.</param>
public sealed record Room(string Code, string Building, int Capacity, TimeSpan? MaintenanceStartUtc, TimeSpan? MaintenanceDuration);

/// <summary>An event returned by the corporate calendar API.</summary>
/// <param name="OrganizerEmail">
/// Directory address of whoever created the event. Null for events booked at the lobby
/// kiosk, where the guest has no directory account.
/// </param>
public sealed record CalendarEvent(string Id, string RoomCode, DateTime StartUtc, DateTime EndUtc, string? OrganizerEmail);

/// <summary>One page of calendar events. Wide windows span several pages.</summary>
public sealed record CalendarEventPage(List<CalendarEvent> Items, bool HasMore, string? NextPageToken);

/// <summary>A confirmed booking of a room for a window.</summary>
public sealed record Reservation(string Id, string RoomCode, DateTime StartUtc, DateTime EndUtc, string OrganizerEmail);

/// <summary>A request that lost the room and is queued for a later attempt.</summary>
public sealed record WaitlistEntry(string Id, string RoomCode, DateTime RequestedStartUtc, DateTime RequestedEndUtc, string RequesterEmail);

/// <summary>A window in which a room is free.</summary>
public sealed record AvailabilitySlot(string RoomCode, DateTime StartUtc, DateTime EndUtc);

/// <summary>Body of a POST /reservations call.</summary>
public sealed record BookingRequest(string RoomCode, DateTime StartUtc, DateTime EndUtc, string OrganizerEmail);

/// <summary>Result of attempting to book a room.</summary>
public sealed record BookingOutcome(bool Booked, string? ReservationId, string Reason)
{
    public static BookingOutcome Confirmed(string reservationId) => new(true, reservationId, "Confirmed.");
    public static BookingOutcome Rejected(string reason) => new(false, null, reason);
}

/// <summary>Result of a single confirmation delivery.</summary>
public sealed record NotificationResult(bool Delivered, string Detail)
{
    public static NotificationResult Sent() => new(true, "Delivered.");
    public static NotificationResult Failed(string detail) => new(false, detail);
}

/// <summary>Aggregate outcome of a confirmation batch.</summary>
public sealed class ConfirmationSummary
{
    public int DeliveredCount { get; set; }
    public int RejectedCount { get; set; }
    public List<string> RejectedReservationIds { get; } = new();
    public bool AllDelivered => RejectedCount == 0;
}
