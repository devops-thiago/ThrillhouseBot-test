namespace HoldsService;

/// <summary>
/// Every minute: expires uncollected ready holds, then sets a copy aside for the next patron
/// on each title that has one on the shelf.
/// </summary>
public sealed class HoldExpiryWorker(
    HoldQueue queue,
    CatalogClient catalog,
    IHoldNotifier notifier,
    HoldsOptions options,
    TimeProvider clock,
    ILogger<HoldExpiryWorker> log) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromMinutes(1));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            await RunOnceAsync(stoppingToken);
        }
    }

    public async Task RunOnceAsync(CancellationToken ct)
    {
        var now = clock.GetUtcNow();
        var expired = queue.ExpireStale(now, options.PickupWindow);
        if (expired.Count > 0)
        {
            log.LogInformation("Expired {Count} uncollected holds", expired.Count);
        }

        var copies = await catalog.AvailableCopiesAsync(options.Branch, ct);
        var shelved = CatalogClient.DistinctIsbns(copies).ToHashSet();

        var holdsWithCopyOnShelf = new List<Hold>();
        foreach (var isbn in queue.WaitingIsbns())
        {
            var next = queue.NextInLine(isbn);
            if (next is null || next.ReadyAt is not null)
            {
                continue;
            }

            var onShelf = shelved.Contains(isbn);
            log.LogDebug("Next hold on {Isbn} (copy on shelf: {OnShelf})", isbn, onShelf);
            holdsWithCopyOnShelf.Add(next);
        }

        if (holdsWithCopyOnShelf.Count == 0)
        {
            return;
        }

        foreach (var hold in holdsWithCopyOnShelf)
        {
            hold.ReadyAt = now;
            await notifier.NotifyReadyAsync(hold, ct);
        }
    }
}
