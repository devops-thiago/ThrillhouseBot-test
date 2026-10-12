using HoldsService;

var builder = WebApplication.CreateBuilder(args);
var options = HoldsOptions.FromEnvironment(Environment.GetEnvironmentVariable);

builder.Services.AddSingleton(options);
builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<HoldQueue>();
builder.Services.AddSingleton<IPatronDirectory>(new PatronDirectory(new Dictionary<string, string>
{
    // Seed records until the patron sync from the ILS lands.
    ["P-1001"] = "adult",
    ["P-1002"] = "child",
}));
builder.Services.AddSingleton<ReservationDesk>();
builder.Services.AddSingleton<IHoldNotifier, LoggingHoldNotifier>();
builder.Services.AddSingleton(new SlipStore(options.SlipRoot));
builder.Services.AddHttpClient<CatalogClient>(http =>
    http.BaseAddress = new Uri("http://catalog.internal:8080/api/"));
builder.Services.AddHostedService<HoldExpiryWorker>();

var app = builder.Build();

app.MapGet("/healthz", () => Results.Ok());

app.MapPost("/holds", (PlaceHoldRequest request, ReservationDesk desk) =>
    desk.PlaceHold(request.PatronId, request.Isbn) == HoldOutcome.Placed
        ? Results.Created($"/holds/{request.Isbn}/next", null)
        : Results.Conflict(new { error = "hold limit reached" }));

app.MapGet("/holds/{isbn}/next", (string isbn, HoldQueue queue) =>
    queue.NextInLine(isbn) is { } hold
        ? Results.Ok(new { hold.PatronId, hold.PlacedAt, hold.ReadyAt })
        : Results.NotFound());

app.MapGet("/patrons/{patronId}/holds", (string patronId, HoldQueue queue) =>
    queue.ListForPatron(patronId).Select(hold => new
    {
        hold.Isbn,
        hold.PlacedAt,
        hold.ReadyAt,
        Position = queue.PositionOf(hold.Isbn, patronId),
    }));

app.MapGet("/branches/{branch}/slips", async (string branch, string name, SlipStore slips, CancellationToken ct) =>
    await slips.ReadAsync(branch, name, ct) is { } bytes
        ? Results.File(bytes, "application/pdf", name)
        : Results.NotFound());

app.Run();

public sealed record PlaceHoldRequest(string PatronId, string Isbn);
