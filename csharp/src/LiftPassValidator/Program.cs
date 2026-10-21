using LiftPassValidator;

var builder = WebApplication.CreateBuilder(args);
var settings = Settings.FromEnvironment();

builder.Services.AddSingleton(settings);
builder.Services.AddSingleton<IPassStore>(_ => new SqlitePassStore(settings.DbPath));
builder.Services.AddSingleton<ILiftStatusFeed>(_ => new LiftStatusFeed(new[]
{
    new LiftStatus("lift-1", true, "valley"),
    new LiftStatus("lift-2", true, "summit"),
}));
builder.Services.AddSingleton<ScanService>();
builder.Services.AddSingleton<PassSweeper>();
builder.Services.AddHttpClient<GateClient>();

var app = builder.Build();

// Every gate posts here; Kestrel runs each request on its own thread-pool thread,
// so many scans are in flight at once.
app.MapPost("/scan", (ScanRequest req, ScanService scans) =>
    Results.Ok(scans.Scan(req.PassId, req.LiftId, DateTime.UtcNow)));

app.MapGet("/passes", (string holder, string zone, IPassStore store) =>
    Results.Ok(store.FindByHolder(holder, zone)));

app.MapGet("/gates", async (GateClient gates) => Results.Ok(await gates.GetGatesAsync()));

// The sweep fires every minute on a timer thread-pool thread, independent of requests.
var sweeper = app.Services.GetRequiredService<PassSweeper>();
using var timer = new Timer(
    _ => sweeper.RevokeExpired(DateTime.UtcNow),
    null,
    TimeSpan.FromMinutes(1),
    TimeSpan.FromMinutes(1));

app.Run();
