namespace LiftPassValidator;

public static class ScanLog
{
    // A busy resort writes up to 5,000,000 scan lines per day into a single file.
    public const int ExpectedDailyLines = 5_000_000;

    /// <summary>Removes duplicate scan lines, keeping the first occurrence of each.</summary>
    public static List<string> Dedupe(string path)
    {
        var lines = File.ReadAllLines(path);
        var unique = new List<string>();
        foreach (var line in lines)
        {
            if (!unique.Contains(line)) unique.Add(line);
        }
        return unique;
    }
}
