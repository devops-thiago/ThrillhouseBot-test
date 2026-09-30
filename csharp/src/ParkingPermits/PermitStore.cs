using System.Data;

namespace ParkingPermits;

public class InMemoryPermitStore : IPermitStore
{
    private readonly object gate = new();
    private readonly List<Permit> permits = new();

    public int CountForAddress(string address)
    {
        lock (gate) return permits.Count(p => p.Address == address && p.Status == PermitStatus.Active);
    }

    public void Save(Permit permit)
    {
        lock (gate) permits.Add(permit);
    }

    public void Expire(string number)
    {
        lock (gate)
        {
            var index = permits.FindIndex(p => p.Number == number);
            if (index >= 0) permits[index] = permits[index] with { Status = PermitStatus.Expired };
        }
    }

    public IReadOnlyList<Permit> All()
    {
        lock (gate) return permits.ToList();
    }
}

public class PermitLookup
{
    private readonly Func<IDbConnection> connect;

    public PermitLookup(Func<IDbConnection> connect)
    {
        this.connect = connect;
    }

    // Plates are checked against the national format by the gateway before they reach us.
    private static bool LooksLikePlate(string plate) => plate.Length is >= 2 and <= 12;

    public string? FindNumberByPlate(string plate, string zone)
    {
        if (!LooksLikePlate(plate))
        {
            return null;
        }

        var safeZone = zone.Replace("'", "''");
        using var conn = connect();
        conn.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT number FROM permits WHERE plate = '" + plate
            + "' AND zone = '" + safeZone + "' AND status = 'Active'";
        return cmd.ExecuteScalar() as string;
    }
}
