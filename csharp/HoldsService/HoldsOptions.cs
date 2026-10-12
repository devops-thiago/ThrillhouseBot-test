namespace HoldsService;

/// <summary>Settings read from the environment at startup. See docs/CONFIG-CSHARP.md.</summary>
public sealed class HoldsOptions
{
    public required string Branch { get; init; }

    public required string SlipRoot { get; init; }

    public required TimeSpan PickupWindow { get; init; }

    public string? CatalogToken { get; init; }

    public static HoldsOptions FromEnvironment(Func<string, string?> env) => new()
    {
        Branch = env("HOLDS_BRANCH") ?? "central",
        SlipRoot = env("HOLDS_SLIP_ROOT") ?? "/var/lib/holds/slips",
        PickupWindow = TimeSpan.FromHours(int.Parse(env("HOLDS_PICKUP_WINDOW") ?? "72")),
        CatalogToken = env("HOLDS_CATALOG_TOKEN"),
    };
}
