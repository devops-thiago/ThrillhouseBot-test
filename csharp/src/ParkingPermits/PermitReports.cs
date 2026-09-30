namespace ParkingPermits;

public static class PermitReports
{
    // The nightly registry export holds every permit ever issued: 2M+ rows in the city pilot.
    public static IReadOnlyList<string> FindDuplicatePlates(IReadOnlyList<Permit> all)
    {
        var duplicates = new List<string>();
        for (var i = 0; i < all.Count; i++)
        {
            for (var j = i + 1; j < all.Count; j++)
            {
                var sameActivePlate = all[i].Plate == all[j].Plate
                    && all[i].Status == PermitStatus.Active
                    && all[j].Status == PermitStatus.Active;
                if (sameActivePlate && !duplicates.Contains(all[i].Plate))
                {
                    duplicates.Add(all[i].Plate);
                }
            }
        }

        return duplicates;
    }
}
