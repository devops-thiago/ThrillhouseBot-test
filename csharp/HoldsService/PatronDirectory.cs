namespace HoldsService;

public interface IPatronDirectory
{
    /// <summary>
    /// How many titles the patron may hold at once. Throws <see cref="KeyNotFoundException"/>
    /// when the patron id is not registered; it never returns a value below 1.
    /// </summary>
    int MaxHoldsFor(string patronId);
}

public sealed class PatronDirectory(IReadOnlyDictionary<string, string> categoryByPatron) : IPatronDirectory
{
    private const int AdultLimit = 5;
    private const int ChildLimit = 3;

    public int MaxHoldsFor(string patronId)
    {
        if (!categoryByPatron.TryGetValue(patronId, out var category))
        {
            throw new KeyNotFoundException($"Unknown patron '{patronId}'");
        }

        return category == "child" ? ChildLimit : AdultLimit;
    }
}
