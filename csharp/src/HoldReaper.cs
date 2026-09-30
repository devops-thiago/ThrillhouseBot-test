namespace RoomBooking;

public sealed class HoldReaper : BackgroundService
{
    private readonly BookingService _service;

    public HoldReaper(BookingService service) => _service = service;

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromMinutes(1));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            _service.ReleaseExpired(DateTime.UtcNow);
        }
    }
}
