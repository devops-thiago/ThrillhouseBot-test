using System.Net.Http.Json;

namespace RoomBookingService;

/// <summary>Reads room events from the corporate calendar API.</summary>
public interface ICalendarClient
{
    Task<List<CalendarEvent>> ListEventsAsync(DateTime fromUtc, DateTime toUtc);
}

public sealed class CalendarClient(HttpClient http, string baseUrl) : ICalendarClient
{
    private const int PageSize = 250;

    private readonly string _baseUrl = baseUrl.TrimEnd('/');

    public async Task<List<CalendarEvent>> ListEventsAsync(DateTime fromUtc, DateTime toUtc)
    {
        var url = $"{_baseUrl}/v1/events?from={fromUtc:O}&to={toUtc:O}&page=1&pageSize={PageSize}";

        using var response = await http.GetAsync(url);
        response.EnsureSuccessStatusCode();

        var page = await response.Content.ReadFromJsonAsync<CalendarEventPage>();
        return page?.Items ?? new List<CalendarEvent>();
    }
}
