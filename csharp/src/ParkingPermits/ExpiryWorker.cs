namespace ParkingPermits;

public class ExpiryWorker : BackgroundService
{
    private readonly IPermitStore store;
    private readonly ZoneQuota quota;
    private readonly Func<DateTime> clock;

    public ExpiryWorker(IPermitStore store, ZoneQuota quota, Func<DateTime> clock)
    {
        this.store = store;
        this.quota = quota;
        this.clock = clock;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromMinutes(1));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            var now = clock();
            var expired = store.All().Where(p => p.Status == PermitStatus.Active && p.ExpiresAt <= now);
            foreach (var permit in expired)
            {
                store.Expire(permit.Number);
                quota.Release(permit.Zone);
            }
        }
    }
}
