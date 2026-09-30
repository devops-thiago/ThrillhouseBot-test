using RoomBooking;

var builder = WebApplication.CreateBuilder(args);
var settings = Settings.FromEnvironment();

builder.Services.AddSingleton(settings);
builder.Services.AddSingleton<INotifier, LoggingNotifier>();
builder.Services.AddSingleton<BookingService>();
builder.Services.AddSingleton<RoomFinder>();
builder.Services.AddSingleton<ReportExporter>();
builder.Services.AddHostedService<HoldReaper>();
builder.Services.AddHttpClient<IRoomApi, CalendarClient>(c =>
    c.BaseAddress = new Uri(Environment.GetEnvironmentVariable("ROOMBOOKING_CALENDAR_URL")
                            ?? "https://calendar.example.internal/api/"));

var app = builder.Build();

app.MapPost("/bookings", (Booking booking, BookingService svc, Settings s) =>
{
    if (!s.IsAllowedEmail(booking.OrganizerEmail)) return Results.BadRequest("email domain not allowed");
    return svc.TryBook(booking) ? Results.Created($"/bookings/{booking.Id}", booking) : Results.Conflict();
});

app.MapGet("/rooms/suggest", async (int attendees, DateTime start, DateTime end,
    IRoomApi api, RoomFinder finder) =>
{
    var rooms = await new RoomDirectory(api).LoadAllAsync();
    var room = finder.Suggest(rooms, attendees, start, end);
    return room is null ? Results.NotFound() : Results.Ok(room);
});

app.MapGet("/reports/{tenant}", (string tenant, string name, string roomId,
    BookingService svc, ReportExporter exporter) =>
    Results.Ok(exporter.Export(tenant, name, svc.ForRoom(roomId))));

app.Run();
