namespace RoomBooking;

public interface INotifier
{
    /// <summary>
    /// Sends a confirmation mail. Throws <see cref="ArgumentException"/> when the
    /// recipient is blank. Returns false if the relay rejects the message.
    /// </summary>
    bool Send(string recipient, string subject);
}

public sealed class LoggingNotifier : INotifier
{
    private readonly ILogger<LoggingNotifier> _log;

    public LoggingNotifier(ILogger<LoggingNotifier> log) => _log = log;

    public bool Send(string recipient, string subject)
    {
        if (string.IsNullOrWhiteSpace(recipient))
            throw new ArgumentException("recipient is required", nameof(recipient));

        _log.LogInformation("mail to {Recipient}: {Subject}", recipient, subject);
        return true;
    }
}
