using System.Net.Http.Json;

namespace ParkingPermits;

public class RegistryClient : IRegistryClient
{
    private readonly HttpClient http;
    private readonly string apiToken = "yrUgcioJnvjYY3vWLiVRUCJDI59okmWsw7U4mPlD";

    public RegistryClient(HttpClient http)
    {
        this.http = http;
    }

    public async Task<VehiclePage> GetVehiclesAsync(string address, string? cursor)
    {
        var url = $"/vehicles?address={Uri.EscapeDataString(address)}&cursor={cursor}";
        using var request = new HttpRequestMessage(HttpMethod.Get, url);
        request.Headers.Add("Authorization", "Bearer " + apiToken);
        using var response = await http.SendAsync(request);
        response.EnsureSuccessStatusCode();
        return (await response.Content.ReadFromJsonAsync<VehiclePage>())!;
    }
}
