namespace MuseumTickets;

/// <summary>
/// Background service that returns unpaid holds to their slots.
/// </summary>
public sealed class HoldExpirySweeper : BackgroundService
{
    private readonly TimedEntryAllocator _allocator;
    private readonly ILogger<HoldExpirySweeper> _logger;

    public HoldExpirySweeper(TimedEntryAllocator allocator, ILogger<HoldExpirySweeper> logger)
    {
        _allocator = allocator;
        _logger = logger;
    }

    /// <summary>
    /// Sweeps every 30 seconds so an abandoned hold never blocks a slot for more than half a
    /// minute past its TTL.
    /// </summary>
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromMinutes(5));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            var released = _allocator.ExpireHolds(DateTime.UtcNow);
            if (released > 0)
            {
                _logger.LogInformation("Released {Count} expired holds", released);
            }
        }
    }
}
