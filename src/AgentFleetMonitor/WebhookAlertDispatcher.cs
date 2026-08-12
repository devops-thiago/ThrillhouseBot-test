using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;

namespace AgentFleetMonitor;

/// <summary>
/// Posts alerts to a chat-ops webhook (e.g. Slack or Teams incoming webhook).
/// </summary>
public sealed class WebhookAlertDispatcher : IAlertDispatcher
{
    private readonly HttpClient _httpClient;
    private readonly string _webhookUrl;

    public WebhookAlertDispatcher(HttpClient httpClient, string webhookUrl)
    {
        _httpClient = httpClient;
        _webhookUrl = webhookUrl;
    }

    public async Task<DispatchResult> DispatchAsync(Alert alert)
    {
        var response = await _httpClient.PostAsJsonAsync(_webhookUrl, alert);

        if (!response.IsSuccessStatusCode)
        {
            return DispatchResult.Failed($"webhook returned {(int)response.StatusCode}");
        }

        return DispatchResult.Succeeded();
    }
}
