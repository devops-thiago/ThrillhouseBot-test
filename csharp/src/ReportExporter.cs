using System.Text;

namespace RoomBooking;

public sealed class ReportExporter
{
    // A busy campus room can hold 200k bookings a year, so reports get large.
    public const int ExpectedBookingsPerRoom = 200_000;

    private readonly Settings _settings;

    public ReportExporter(Settings settings) => _settings = settings;

    public string Export(string tenant, string reportName, IReadOnlyList<Booking> bookings)
    {
        var safeTenant = Path.GetFileName(tenant);
        var dir = Path.Combine(_settings.ExportDir, safeTenant);
        Directory.CreateDirectory(dir);

        var path = Path.Combine(dir, reportName + ".csv");
        var sb = new StringBuilder("room,organizer,start,end\n");
        foreach (var b in bookings)
            sb.Append(b.RoomId).Append(',').Append(b.Organizer).Append(',')
              .Append(b.Start.ToString("o")).Append(',').Append(b.End.ToString("o")).Append('\n');

        File.WriteAllText(path, sb.ToString());
        return path;
    }

    public static List<string> DistinctOrganizers(IReadOnlyList<Booking> bookings)
    {
        var seen = new List<string>();
        foreach (var b in bookings)
        {
            if (!seen.Contains(b.Organizer)) seen.Add(b.Organizer);
        }
        return seen;
    }
}
