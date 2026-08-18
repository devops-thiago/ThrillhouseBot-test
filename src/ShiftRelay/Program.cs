using ShiftRelay;

// How far ahead the schedule endpoint and the swap import look.
const int HorizonDays = 30;

// The on-call platform renders the rota catalog and mounts it read-only into the
// container; this service only reads it.
const string RotaCatalogPath = "/etc/shiftrelay/rotas.json";

// Settings come from the environment. See docs/CONFIG-CSHARP.md.
var workforceBaseUrl = RequireEnv("SHIFTRELAY_WORKFORCE_BASE_URL");
var connectionString = RequireEnv("SHIFTRELAY_DB_CONNECTION_STRING");
var chatWebhookUrl = RequireEnv("SHIFTRELAY_CHAT_WEBHOOK_URL");
var ackTimeout = TimeSpan.FromMinutes(int.Parse(Environment.GetEnvironmentVariable("SHIFTRELAY_ACK_TIMEOUT_MINUTES") ?? "20"));

var catalog = RotaCatalog.Load(RotaCatalogPath);
var http = new HttpClient();
var repository = new HandoverRepository(connectionString);
var planner = new ShiftPlanner();
var coordinator = new HandoverCoordinator(repository, planner);
var broadcast = new HandoverBroadcast(new ChatHandoverAnnouncer(http, chatWebhookUrl));
var importer = new ShiftSwapImporter(new WorkforceClient(http, workforceBaseUrl), repository);
var sweeper = new EscalationSweeper(repository, broadcast, ackTimeout, Console.WriteLine);

var app = WebApplication.CreateBuilder(args).Build();

app.MapPost("/handovers", async (HandoverRequest request) =>
{
    var rota = catalog.Find(request.RotaCode);
    if (rota is null)
    {
        return Results.NotFound($"Unknown rota {request.RotaCode}.");
    }

    var outcome = await coordinator.RecordAsync(rota, request);
    if (!outcome.Accepted)
    {
        return Results.Conflict(outcome);
    }

    var handover = await repository.FindAsync(outcome.HandoverId!);
    await broadcast.AnnounceAllAsync([handover!]);
    return Results.Ok(outcome);
});

app.MapPost("/handovers/{id}/acknowledge", async (string id, string engineer) =>
{
    var outcome = await coordinator.AcknowledgeAsync(id, engineer);
    return outcome.Accepted ? Results.Ok(outcome) : Results.Conflict(outcome);
});

app.MapGet("/rotas/{code}/holder", async (string code) =>
{
    var rota = catalog.Find(code);
    if (rota is null)
    {
        return Results.NotFound();
    }

    var nowUtc = DateTime.UtcNow;
    var overrides = await repository.FindOverridesAsync(rota.Code, nowUtc.AddDays(-1), nowUtc.AddDays(1));
    var holder = planner.HolderAt(rota, overrides, nowUtc);
    return holder is null ? Results.NotFound() : Results.Ok(holder);
});

app.MapGet("/rotas/{code}/schedule", async (string code, int? days) =>
{
    var rota = catalog.Find(code);
    if (rota is null)
    {
        return Results.NotFound();
    }

    var horizon = days ?? HorizonDays;
    var fromUtc = DateTime.UtcNow;
    var overrides = await repository.FindOverridesAsync(rota.Code, fromUtc.AddDays(-1), fromUtc.AddDays(horizon));
    return Results.Ok(planner.BuildSchedule(rota, overrides, fromUtc, horizon));
});

app.MapPost("/sync", async () =>
{
    var fromUtc = DateTime.UtcNow;
    return Results.Ok(new { imported = await importer.ImportAsync(fromUtc, fromUtc.AddDays(HorizonDays)) });
});

app.MapGet("/rotas", () => Results.Ok(catalog.All));

// Handovers nobody picked up are escalated on a timer rather than on request, so an
// engineer who has gone to bed still gets chased in the channel.
using var escalationTimer = new Timer(async _ => await sweeper.RunOnceAsync(), null, TimeSpan.FromMinutes(1), TimeSpan.FromMinutes(5));

app.Run();

static string RequireEnv(string name)
{
    var value = Environment.GetEnvironmentVariable(name);
    if (string.IsNullOrWhiteSpace(value))
    {
        throw new InvalidOperationException($"Missing required environment variable {name}.");
    }

    return value;
}
