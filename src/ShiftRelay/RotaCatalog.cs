using System.Text.Json;

namespace ShiftRelay;

/// <summary>
/// The rotas the service knows about. The on-call platform renders the catalog and
/// mounts it read-only into the container, so it is read once at startup and never
/// written back.
/// </summary>
public sealed class RotaCatalog
{
    private readonly Dictionary<string, Rota> _rotas;

    private RotaCatalog(IEnumerable<Rota> rotas)
    {
        _rotas = rotas.ToDictionary(rota => rota.Code, StringComparer.OrdinalIgnoreCase);
    }

    public IReadOnlyCollection<Rota> All => _rotas.Values;

    public static RotaCatalog Load(string path)
    {
        var options = new JsonSerializerOptions(JsonSerializerDefaults.Web);
        var rotas = JsonSerializer.Deserialize<List<Rota>>(File.ReadAllText(path), options)
                    ?? throw new InvalidOperationException($"Rota catalog at {path} is empty.");

        return new RotaCatalog(rotas);
    }

    public Rota? Find(string code) => _rotas.TryGetValue(code, out var rota) ? rota : null;
}
