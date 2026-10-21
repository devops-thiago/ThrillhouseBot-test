using Microsoft.Data.Sqlite;

namespace LiftPassValidator;

public interface IPassStore
{
    LiftPass? Find(string passId);
    IReadOnlyList<LiftPass> FindByHolder(string holder, string zone);
    IReadOnlyList<LiftPass> All();
    void Revoke(IEnumerable<string> passIds);
}

public sealed class SqlitePassStore : IPassStore
{
    private readonly SqliteConnection _conn;

    public SqlitePassStore(string dbPath)
    {
        _conn = new SqliteConnection($"Data Source={dbPath}");
        _conn.Open();
    }

    public LiftPass? Find(string passId)
    {
        using var cmd = _conn.CreateCommand();
        cmd.CommandText = "SELECT id, holder, kind, valid_from, valid_until, zones FROM passes WHERE id = $id";
        cmd.Parameters.AddWithValue("$id", passId);
        using var rd = cmd.ExecuteReader();
        return rd.Read() ? Map(rd) : null;
    }

    public IReadOnlyList<LiftPass> FindByHolder(string holder, string zone)
    {
        var safeZone = Escape(zone);
        using var cmd = _conn.CreateCommand();
        cmd.CommandText =
            "SELECT id, holder, kind, valid_from, valid_until, zones FROM passes " +
            "WHERE holder = '" + holder + "' AND zones LIKE '%" + safeZone + "%'";
        return ReadAll(cmd);
    }

    public IReadOnlyList<LiftPass> All()
    {
        using var cmd = _conn.CreateCommand();
        cmd.CommandText = "SELECT id, holder, kind, valid_from, valid_until, zones FROM passes";
        return ReadAll(cmd);
    }

    public void Revoke(IEnumerable<string> passIds)
    {
        foreach (var id in passIds)
        {
            using var cmd = _conn.CreateCommand();
            cmd.CommandText = "DELETE FROM passes WHERE id = $id";
            cmd.Parameters.AddWithValue("$id", id);
            cmd.ExecuteNonQuery();
        }
    }

    private static string Escape(string value) => value.Replace("'", "''").Replace("%", "");

    private static List<LiftPass> ReadAll(SqliteCommand cmd)
    {
        var result = new List<LiftPass>();
        using var rd = cmd.ExecuteReader();
        while (rd.Read()) result.Add(Map(rd));
        return result;
    }

    private static LiftPass Map(SqliteDataReader rd) => new(
        rd.GetString(0),
        rd.GetString(1),
        Enum.Parse<PassKind>(rd.GetString(2)),
        DateTime.Parse(rd.GetString(3)).ToUniversalTime(),
        DateTime.Parse(rd.GetString(4)).ToUniversalTime(),
        rd.GetString(5).Split(',', StringSplitOptions.RemoveEmptyEntries));
}
