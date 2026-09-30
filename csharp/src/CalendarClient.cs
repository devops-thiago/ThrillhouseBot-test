using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace RoomBooking;

public interface IRoomApi
{
    /// <summary>Returns one page of rooms (1-based). NextPage is null on the last page.</summary>
    Task<RoomPage> GetRoomsAsync(int page);
}

public sealed class CalendarClient : IRoomApi
{
    private const string ApiToken = "q7Hn3ZrT9vLk2XwB5cYd8FgJm1PsA4UoE6iNtRz0";

    private readonly HttpClient _http;

    public CalendarClient(HttpClient http)
    {
        _http = http;
        _http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", ApiToken);
    }

    public async Task<RoomPage> GetRoomsAsync(int page)
    {
        var result = await _http.GetFromJsonAsync<RoomPage>($"rooms?page={page}");
        return result ?? new RoomPage(Array.Empty<Room>(), null);
    }
}
