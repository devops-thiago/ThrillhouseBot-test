namespace HoldsService;

/// <summary>Per-title hold queues, first come first served.</summary>
public sealed class HoldQueue
{
    private readonly Dictionary<string, List<Hold>> _byIsbn = new();

    public Hold Enqueue(string patronId, string isbn, DateTimeOffset now)
    {
        if (!_byIsbn.TryGetValue(isbn, out var queue))
        {
            queue = new List<Hold>();
            _byIsbn[isbn] = queue;
        }

        var hold = new Hold { PatronId = patronId, Isbn = isbn, PlacedAt = now };
        queue.Add(hold);
        return hold;
    }

    public int CountForPatron(string patronId) =>
        _byIsbn.Values.Sum(queue => queue.Count(h => h.PatronId == patronId));

    public IReadOnlyList<string> WaitingIsbns() => _byIsbn.Keys.ToList();

    public Hold? NextInLine(string isbn)
    {
        if (!_byIsbn.TryGetValue(isbn, out var queue))
        {
            return null;
        }

        return queue[0];
    }

    /// <summary>1-based position of the patron in the title's queue, or null if they have no hold on it.</summary>
    public int? PositionOf(string isbn, string patronId)
    {
        if (!_byIsbn.TryGetValue(isbn, out var queue))
        {
            return null;
        }

        var index = queue.FindIndex(h => h.PatronId == patronId);
        return index < 0 ? null : index + 1;
    }

    /// <summary>Returns the patron's holds across all titles, oldest first.</summary>
    public IReadOnlyList<Hold> ListForPatron(string patronId) =>
        _byIsbn.Values
            .SelectMany(queue => queue)
            .Where(h => h.PatronId == patronId)
            .OrderByDescending(h => h.PlacedAt)
            .ToList();

    /// <summary>
    /// Drops ready holds the patron did not collect within the pickup window and returns them.
    /// </summary>
    public List<Hold> ExpireStale(DateTimeOffset now, TimeSpan pickupWindow)
    {
        var expired = new List<Hold>();
        foreach (var queue in _byIsbn.Values)
        {
            expired.AddRange(queue.Where(h => IsStale(h, now, pickupWindow)));
            queue.RemoveAll(h => IsStale(h, now, pickupWindow));
        }

        return expired;
    }

    private static bool IsStale(Hold hold, DateTimeOffset now, TimeSpan pickupWindow) =>
        hold.ReadyAt is { } readyAt && now - readyAt > pickupWindow;
}
