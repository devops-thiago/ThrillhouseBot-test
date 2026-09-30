namespace RoomBooking;

public sealed class Settings
{
    public IReadOnlyList<string> AllowedDomains { get; init; } = Array.Empty<string>();
    public TimeSpan HoldTtl { get; init; } = TimeSpan.FromMinutes(15);
    public string ExportDir { get; init; } = "/var/lib/roombooking/exports";

    // Reads ROOMBOOKING_* environment variables; unset values keep their defaults.
    public static Settings FromEnvironment()
    {
        var domains = Environment.GetEnvironmentVariable("ROOMBOOKING_ALLOWED_DOMAINS") ?? "";
        var ttl = Environment.GetEnvironmentVariable("ROOMBOOKING_HOLD_TTL");
        var dir = Environment.GetEnvironmentVariable("ROOMBOOKING_EXPORT_DIR");

        return new Settings
        {
            AllowedDomains = domains
                .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries),
            HoldTtl = ttl is null ? TimeSpan.FromMinutes(15) : TimeSpan.FromMinutes(int.Parse(ttl)),
            ExportDir = dir ?? "/var/lib/roombooking/exports",
        };
    }

    public bool IsAllowedEmail(string email)
    {
        var at = email.LastIndexOf('@');
        if (at < 0) return false;
        return AllowedDomains.Count == 0 ||
               AllowedDomains.Contains(email[(at + 1)..], StringComparer.OrdinalIgnoreCase);
    }
}
