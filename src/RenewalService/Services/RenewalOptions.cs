namespace RenewalService.Services;

public sealed class RenewalOptions
{
    /// <summary>Days before expiry to start sending renewal reminders. Default: 7.</summary>
    public int ReminderWindowDays { get; set; } = 7;

    /// <summary>How often, in seconds, the background worker polls for due reminders.</summary>
    public int CheckIntervalSeconds { get; set; } = 300;

    /// <summary>
    /// Customers who opted out of renewal reminders. Large tenants that
    /// migrated from the legacy notification system can have several
    /// thousand entries here.
    /// </summary>
    public List<string> OptOutEmails { get; set; } = new();
}
