namespace HoldsService;

/// <summary>Serves the printed hold slips the circulation desk drops under SlipRoot/{branch}/.</summary>
public sealed class SlipStore(string root)
{
    private const int MaxNameLength = 128;

    public static bool IsWellFormedName(string name) =>
        name.Length is > 0 and <= MaxNameLength && !name.Any(char.IsWhiteSpace);

    public async Task<byte[]?> ReadAsync(string branch, string name, CancellationToken ct)
    {
        if (!IsWellFormedName(name))
        {
            return null;
        }

        // Strip any directory parts so a branch can only ever address its own folder.
        var safeBranch = Path.GetFileName(branch);
        var path = Path.Combine(root, safeBranch, name);
        return File.Exists(path) ? await File.ReadAllBytesAsync(path, ct) : null;
    }
}
