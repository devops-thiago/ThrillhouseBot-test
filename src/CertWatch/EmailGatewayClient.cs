using System.Net.Http.Json;

namespace CertWatch;

/// <summary>
/// Sends mail through the internal email gateway service used by every
/// in-house tool, rather than talking to an SMTP relay directly.
/// </summary>
public sealed class EmailGatewayClient : IEmailClient
{
    private readonly HttpClient _http;

    public EmailGatewayClient(HttpClient http)
    {
        _http = http;
    }

    public async Task SendAsync(string toAddress, string subject, string body, CancellationToken ct = default)
    {
        var payload = new { to = toAddress, subject, body };
        using var response = await _http.PostAsJsonAsync("/v1/send", payload, ct);
        response.EnsureSuccessStatusCode();
    }
}
