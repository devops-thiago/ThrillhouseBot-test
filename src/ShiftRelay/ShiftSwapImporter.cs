namespace ShiftRelay;

/// <summary>Copies the agreed swaps out of workforce and into the local override table.</summary>
public sealed class ShiftSwapImporter(WorkforceClient client, HandoverRepository repository)
{
    public async Task<int> ImportAsync(DateTime fromUtc, DateTime toUtc)
    {
        var imported = 0;
        string? pageToken = null;

        do
        {
            var page = await client.GetOverridesAsync(fromUtc, toUtc, pageToken);
            foreach (var swap in page.Items)
            {
                await repository.UpsertOverrideAsync(swap);
                imported++;
            }

            pageToken = page.NextPageToken;
        }
        while (!string.IsNullOrEmpty(pageToken));

        return imported;
    }
}
