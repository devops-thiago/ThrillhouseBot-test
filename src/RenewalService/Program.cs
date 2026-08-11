using RenewalService.Data;
using RenewalService.Services;

var builder = WebApplication.CreateBuilder(args);

// RENEWAL_OPT_OUT_EMAILS is a comma-separated list of customer email
// addresses that should never receive a renewal reminder.
var optOutEmails = (builder.Configuration["RENEWAL_OPT_OUT_EMAILS"] ?? string.Empty)
    .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
    .ToList();

var options = new RenewalOptions
{
    ReminderWindowDays = int.TryParse(builder.Configuration["RENEWAL_REMINDER_WINDOW_DAYS"], out var window) ? window : 7,
    CheckIntervalSeconds = int.TryParse(builder.Configuration["RENEWAL_CHECK_INTERVAL"], out var interval) ? interval : 300,
    OptOutEmails = optOutEmails,
};

builder.Services.AddSingleton(options);
builder.Services.AddSingleton<ICustomerRepository, CustomerRepository>();
builder.Services.AddHttpClient<ISubscriptionGateway, SubscriptionGateway>(client =>
{
    client.BaseAddress = new Uri(builder.Configuration["BILLING_API_BASE_URL"] ?? "https://billing.internal/");
});
builder.Services.AddSingleton<IReminderNotifier, EmailReminderNotifier>();
builder.Services.AddSingleton<RenewalProcessor>();
builder.Services.AddHostedService<RenewalReminderWorker>();

var app = builder.Build();

app.MapGet("/health", () => Results.Ok("healthy"));

app.MapGet("/customers/lookup", async (string email, ICustomerRepository repository, CancellationToken ct) =>
{
    var customer = await repository.FindByEmailAsync(email, ct);
    return customer is null ? Results.NotFound() : Results.Ok(customer);
});

app.MapGet("/renewals/count", (RenewalProcessor processor) =>
{
    // Quick synchronous count for the ops dashboard widget.
    var count = processor.CountActiveAsync().Result;
    return Results.Ok(new { count });
});

app.Run();
