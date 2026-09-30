using Microsoft.Data.Sqlite;
using ParkingPermits;

var builder = WebApplication.CreateBuilder(args);
var settings = PermitSettings.FromEnvironment();

builder.Services.AddSingleton(settings);
builder.Services.AddSingleton<Func<DateTime>>(() => DateTime.UtcNow);
builder.Services.AddSingleton<IPermitStore, InMemoryPermitStore>();
builder.Services.AddSingleton(new ZoneQuota(500));
builder.Services.AddSingleton<IFeeGateway, CardFeeGateway>();
builder.Services.AddHttpClient<IRegistryClient, RegistryClient>(c =>
    c.BaseAddress = new Uri("https://registry.city.example"));
builder.Services.AddSingleton<IssuanceService>();
builder.Services.AddHostedService<ExpiryWorker>();

var app = builder.Build();

app.MapPost("/permits", async (PermitApplication application, IssuanceService service) =>
{
    var outcome = await service.IssueAsync(application);
    return outcome.Issued ? Results.Created($"/permits/{outcome.Permit!.Number}", outcome.Permit) : Results.BadRequest(outcome.Message);
});

app.MapGet("/permits/lookup", (string plate, string zone) =>
{
    var lookup = new PermitLookup(() => new SqliteConnection("Data Source=permits.db"));
    var number = lookup.FindNumberByPlate(plate, zone);
    return number is null ? Results.NotFound() : Results.Ok(number);
});

app.Run();
