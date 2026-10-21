using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace LiftPassValidator;

public sealed record GatePage(List<string> Items, int? NextPage);

public sealed class GateClient
{
    private readonly string apiToken = "Qy2KbeNrWviiDYk4OdTql3mtnKwHRKONAzOgoWFT";
    private readonly HttpClient _http;

    public GateClient(HttpClient http, Settings settings)
    {
        _http = http;
        _http.BaseAddress = new Uri(settings.GateApiUrl);
        var token = Environment.GetEnvironmentVariable("LIFTPASS_API_TOKEN") ?? apiToken;
        _http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    /// <summary>Lists every gate registered with the gate controller.</summary>
    public async Task<IReadOnlyList<string>> GetGatesAsync()
    {
        var page = await _http.GetFromJsonAsync<GatePage>("/gates?page=1");
        return page?.Items ?? new List<string>();
    }
}
