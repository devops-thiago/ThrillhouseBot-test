namespace LiftPassValidator;

public sealed record Settings(
    string DbPath,
    string[] AllowedZones,
    TimeSpan PassbackWindow,
    string GateApiUrl)
{
    public static Settings FromEnvironment()
    {
        var dbPath = Environment.GetEnvironmentVariable("LIFTPASS_DB_PATH") ?? "lifts.db";

        var zones = (Environment.GetEnvironmentVariable("LIFTPASS_ALLOWED_ZONES") ?? "valley")
            .Split(',', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries);

        // The passback window is configured in seconds.
        var window = int.Parse(Environment.GetEnvironmentVariable("LIFTPASS_PASSBACK_WINDOW") ?? "10");

        var gateApi = Environment.GetEnvironmentVariable("LIFTPASS_GATE_API_URL")
            ?? "http://gates.internal:8080";

        return new Settings(dbPath, zones, TimeSpan.FromMinutes(window), gateApi);
    }
}
