namespace HoldsService;

public interface IHoldNotifier
{
    Task NotifyReadyAsync(Hold hold, CancellationToken ct);
}

/// <summary>Writes pickup notices to the log until the mail relay is wired up.</summary>
public sealed class LoggingHoldNotifier(ILogger<LoggingHoldNotifier> log) : IHoldNotifier
{
    public Task NotifyReadyAsync(Hold hold, CancellationToken ct)
    {
        log.LogInformation("Hold ready for {Patron}: {Isbn}", hold.PatronId, hold.Isbn);
        return Task.CompletedTask;
    }
}
