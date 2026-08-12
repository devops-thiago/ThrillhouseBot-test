using System.Text;
using WebhookRelay.Models;

namespace WebhookRelay.Services;

/// <summary>
/// Delivers webhook payloads over HTTP using an injected <see cref="HttpClient"/>.
/// </summary>
public class HttpWebhookSender : IWebhookSender
{
    private readonly HttpClient _httpClient;

    public HttpWebhookSender(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public async Task<DeliveryResult> SendAsync(string url, string payload, string signature, CancellationToken ct)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = new StringContent(payload, Encoding.UTF8, "application/json")
        };
        request.Headers.Add("X-Webhook-Signature", signature);

        try
        {
            var response = await _httpClient.SendAsync(request, ct);
            return new DeliveryResult(
                response.IsSuccessStatusCode,
                (int)response.StatusCode,
                response.IsSuccessStatusCode ? null : $"Subscriber returned {(int)response.StatusCode}");
        }
        catch (TaskCanceledException) when (!ct.IsCancellationRequested)
        {
            return new DeliveryResult(false, null, "Request timed out");
        }
        catch (HttpRequestException ex)
        {
            return new DeliveryResult(false, null, ex.Message);
        }
    }
}
