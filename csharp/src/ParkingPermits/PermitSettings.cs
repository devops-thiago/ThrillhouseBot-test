namespace ParkingPermits;

public class PermitSettings
{
    public HashSet<string> AllowedZones { get; init; } = new();
    public int ValidityDays { get; init; } = 365;
    public int MaxPerAddress { get; init; } = 2;
    public decimal AnnualFee { get; init; } = 45m;

    public static PermitSettings FromEnvironment()
    {
        var zones = Environment.GetEnvironmentVariable("PERMIT_ALLOWED_ZONES") ?? "A";
        return new PermitSettings
        {
            AllowedZones = zones
                .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                .ToHashSet(StringComparer.OrdinalIgnoreCase),
            ValidityDays = ReadInt("PERMIT_VALIDITY_DAYS", 365),
            MaxPerAddress = ReadInt("PERMIT_MAX_PER_ADDRESS", 2),
        };
    }

    private static int ReadInt(string name, int fallback)
    {
        var raw = Environment.GetEnvironmentVariable(name);
        return int.TryParse(raw, out var value) ? value : fallback;
    }
}
