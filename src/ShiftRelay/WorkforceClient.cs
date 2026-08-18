using System.Net.Http.Json;

namespace ShiftRelay;

/// <summary>Reads agreed shift swaps from the workforce API.</summary>
public sealed class WorkforceClient(HttpClient http, string baseUrl)
{
    public async Task<ShiftOverridePage> GetOverridesAsync(DateTime fromUtc, DateTime toUtc, string? pageToken)
    {
        var url = $"{baseUrl.TrimEnd('/')}/v2/shift-swaps?from={fromUtc:O}&to={toUtc:O}";
        if (!string.IsNullOrEmpty(pageToken))
        {
            url += $"&pageToken={pageToken}";
        }

        var page = await http.GetFromJsonAsync<ShiftOverridePage>(url);
        return page ?? new ShiftOverridePage([], null);
    }
}
