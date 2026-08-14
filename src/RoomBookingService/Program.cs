using System.Text.Json;
using RoomBookingService;

const int HorizonDays = 90;

// The facilities platform publishes the room catalog and mounts it read-only into
// the container at this path; this service only reads it.
const string RoomCatalogPath = "/etc/roombooking/rooms.json";

// Settings are read from the environment. See docs/CONFIG-CSHARP.md.
var calendarBaseUrl = RequireEnv("ROOMBOOKING_CALENDAR_BASE_URL");
var connectionString = RequireEnv("ROOMBOOKING_DB_CONNECTION_STRING");
var mailGatewayUrl = RequireEnv("ROOMBOOKING_MAIL_GATEWAY_URL");
var holdTtl = int.Parse(Environment.GetEnvironmentVariable("ROOMBOOKING_HOLD_TTL") ?? "120");

var rooms = JsonSerializer.Deserialize<List<Room>>(File.ReadAllText(RoomCatalogPath)) ?? new List<Room>();

var http = new HttpClient();
var repository = new ReservationRepository(connectionString);
var coordinator = new BookingCoordinator(repository, holdTtl);
var planner = new AvailabilityPlanner(HorizonDays);
var importer = new ScheduleImporter(new CalendarClient(http, calendarBaseUrl), repository);
var promoter = new WaitlistPromoter(coordinator, repository, Console.WriteLine);
var confirmations = new ReservationConfirmationSender(new EmailReservationNotifier(http, mailGatewayUrl));

var app = WebApplication.CreateBuilder(args).Build();

app.MapPost("/reservations", async (BookingRequest request) =>
{
    var outcome = await coordinator.TryBookAsync(request.RoomCode, request.StartUtc, request.EndUtc, request.OrganizerEmail);
    if (!outcome.Booked) return Results.Conflict(outcome);

    var reservation = new Reservation(outcome.ReservationId!, request.RoomCode, request.StartUtc, request.EndUtc, request.OrganizerEmail);
    await confirmations.SendAllAsync([reservation]);
    return Results.Ok(outcome);
});

app.MapGet("/rooms/{code}/availability", async (string code) =>
{
    var room = rooms.FirstOrDefault(r => r.Code == code);
    if (room is null) return Results.NotFound();

    var fromUtc = DateTime.UtcNow;
    var reservations = await repository.FindByRoomAsync(code, fromUtc, fromUtc.AddDays(HorizonDays));
    return Results.Ok(planner.BuildAvailability(room, reservations, fromUtc));
});

app.MapPost("/sync", async () =>
    Results.Ok(await importer.ImportAsync(DateTime.UtcNow.AddDays(-1), DateTime.UtcNow.AddDays(HorizonDays))));

// Queued requests are retried on a timer, so a cancellation hands the room to the
// next person without anyone having to poll the API.
using var promotionTimer = new Timer(async _ => await promoter.RunOnceAsync(), null, TimeSpan.FromSeconds(30), TimeSpan.FromMinutes(1));

app.Run();

static string RequireEnv(string name)
{
    var value = Environment.GetEnvironmentVariable(name);
    if (string.IsNullOrWhiteSpace(value)) throw new InvalidOperationException($"Missing required environment variable {name}.");

    return value;
}
