namespace HoldsService;

/// <summary>A patron's place in the queue for one title (by ISBN).</summary>
public sealed class Hold
{
    public required string PatronId { get; init; }

    public required string Isbn { get; init; }

    public DateTimeOffset PlacedAt { get; init; }

    /// <summary>Set when a copy is set aside on the pickup shelf for this patron.</summary>
    public DateTimeOffset? ReadyAt { get; set; }
}

public enum HoldOutcome
{
    Placed,
    Rejected,
}
