using MuseumTickets;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton(AllocatorConfig.FromEnvironment());
builder.Services.AddSingleton<TimedEntryAllocator>();
builder.Services.AddSingleton<IGalleryCatalog, GalleryCatalog>();
builder.Services.AddSingleton<ReceiptBuilder>();
builder.Services.AddSingleton<TicketTemplateRenderer>();
builder.Services.AddHttpClient<GroupBookingClient>();
builder.Services.AddHostedService<HoldExpirySweeper>();

var app = builder.Build();

app.MapPost("/holds", (HoldRequest request, TimedEntryAllocator allocator) =>
{
    var hold = allocator.TryHold(request, DateTime.UtcNow);
    if (hold is not null)
    {
        return Results.Created($"/holds/{hold.Id}", hold);
    }

    var alternatives = allocator.SlotsWithCapacity(
        request.Gallery, DateOnly.FromDateTime(request.Slot), request.Quantity);
    if (alternatives.Count == 0)
    {
        return Results.Conflict(new { error = "sold out for the day" });
    }

    return Results.Conflict(new { error = "slot full", alternatives });
});

app.MapGet("/holds/{id:guid}/receipt", (Guid id, TimedEntryAllocator allocator, ReceiptBuilder receipts) =>
{
    var hold = allocator.ActiveHolds.FirstOrDefault(h => h.Id == id);
    return hold is null ? Results.NotFound() : Results.Text(receipts.Build(hold));
});

app.MapGet("/tickets/print", (string gallery, string template, TicketTemplateRenderer renderer) =>
    Results.Content(renderer.Render(new PrintRequest(gallery, template)), "text/html"));

app.MapPost("/group-bookings/sync", async (
    DateOnly day, GroupBookingClient client, TimedEntryAllocator allocator, CancellationToken ct) =>
{
    var bookings = await client.FetchForDayAsync(day, ct);
    var placed = 0;
    foreach (var booking in bookings)
    {
        var request = new HoldRequest(booking.Gallery, booking.Slot, booking.Quantity, booking.Organiser);
        if (allocator.TryHold(request, DateTime.UtcNow) is not null)
        {
            placed++;
        }
    }

    return Results.Ok(new { fetched = bookings.Count, placed });
});

app.Run();

public partial class Program;
