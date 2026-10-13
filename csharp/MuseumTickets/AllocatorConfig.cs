namespace MuseumTickets;

/// <summary>
/// Runtime settings for the timed-entry allocator, read from environment variables.
/// </summary>
public sealed record AllocatorConfig(
    IReadOnlyList<string> AllowedGalleries,
    TimeSpan HoldTtl,
    Uri PartnerApiUrl,
    string? PartnerApiToken,
    string TemplateRoot)
{
    public const int DefaultSlotCapacity = 40;
    public const int OpeningHour = 9;
    public const int ClosingHour = 17;

    public static AllocatorConfig FromEnvironment()
    {
        var galleries = (Environment.GetEnvironmentVariable("ALLOWED_GALLERIES") ?? "main-hall")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);

        var ttlSeconds = int.TryParse(Environment.GetEnvironmentVariable("HOLD_TTL_SECONDS"), out var parsed)
            ? parsed
            : 600;

        var partnerUrl = Environment.GetEnvironmentVariable("PARTNER_API_URL")
            ?? "https://partners.example-museum.org/api/";

        return new AllocatorConfig(
            galleries,
            TimeSpan.FromSeconds(ttlSeconds),
            new Uri(partnerUrl),
            Environment.GetEnvironmentVariable("PARTNER_API_TOKEN"),
            Path.Combine(AppContext.BaseDirectory, "templates"));
    }
}
