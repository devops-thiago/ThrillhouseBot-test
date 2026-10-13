namespace MuseumTickets;

public interface IGalleryCatalog
{
    /// <summary>
    /// Returns the public display name of a gallery. Throws <see cref="KeyNotFoundException"/>
    /// when the id is not registered; never returns null.
    /// </summary>
    string DisplayName(string galleryId);
}

public sealed class GalleryCatalog : IGalleryCatalog
{
    private static readonly Dictionary<string, string> Names = new()
    {
        ["main-hall"] = "Main Hall",
        ["egyptian-wing"] = "Egyptian Wing",
        ["modern-art"] = "Modern Art Gallery",
    };

    public string DisplayName(string galleryId) =>
        Names.TryGetValue(galleryId, out var name)
            ? name
            : throw new KeyNotFoundException($"Unknown gallery '{galleryId}'");
}

public sealed class ReceiptBuilder
{
    private readonly IGalleryCatalog _catalog;

    public ReceiptBuilder(IGalleryCatalog catalog)
    {
        _catalog = catalog;
    }

    public string Build(Hold hold)
    {
        var name = _catalog.DisplayName(hold.Gallery);
        if (name is null)
        {
            return $"Hold {hold.Id}: {hold.Quantity} ticket(s), gallery unavailable";
        }

        return $"Hold {hold.Id}: {hold.Quantity} ticket(s) for {name} at {hold.Slot:HH:mm}, " +
               $"pay before {hold.ExpiresAt:HH:mm} UTC";
    }
}
