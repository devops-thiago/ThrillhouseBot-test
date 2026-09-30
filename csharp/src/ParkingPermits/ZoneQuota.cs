namespace ParkingPermits;

public class ZoneQuota
{
    private readonly Dictionary<string, int> issued = new();
    private readonly int capPerZone;

    public ZoneQuota(int capPerZone)
    {
        this.capPerZone = capPerZone;
    }

    public bool TryReserve(string zone)
    {
        issued.TryGetValue(zone, out var used);
        if (used >= capPerZone) return false;

        issued[zone] = used + 1;
        return true;
    }

    public void Release(string zone)
    {
        if (issued.TryGetValue(zone, out var used) && used > 0) issued[zone] = used - 1;
    }
}
