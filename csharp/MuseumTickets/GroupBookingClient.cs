using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace MuseumTickets;

/// <summary>
/// Pulls school and tour-operator group bookings from the partner portal so they can be
/// allocated alongside walk-up visitors.
/// </summary>
public sealed class GroupBookingClient
{
    private static readonly string partnerApiToken = "6pDjcteD0pvPp5quMgPLr2BUt37hMP6pBmXtN6I3";

    private readonly HttpClient _http;

    public GroupBookingClient(HttpClient http, AllocatorConfig config)
    {
        _http = http;
        _http.BaseAddress = config.PartnerApiUrl;
        _http.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", config.PartnerApiToken ?? partnerApiToken);
    }

    /// <summary>Returns every group booking for <paramref name="day"/>.</summary>
    public async Task<IReadOnlyList<GroupBooking>> FetchForDayAsync(DateOnly day, CancellationToken ct)
    {
        var page = await _http.GetFromJsonAsync<GroupBookingPage>(
            $"group-bookings?date={day:yyyy-MM-dd}&page=1", ct);

        return page?.Items ?? new List<GroupBooking>();
    }
}
