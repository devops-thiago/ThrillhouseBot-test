using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;

namespace AgentFleetMonitor;

public sealed class AgentFleetClient : IAgentFleetClient
{
    private readonly HttpClient _httpClient;
    private readonly string _baseUrl;

    public AgentFleetClient(HttpClient httpClient, string baseUrl)
    {
        _httpClient = httpClient;
        _baseUrl = baseUrl.TrimEnd('/');
    }

    /// <summary>
    /// Returns every agent currently registered with the fleet.
    /// </summary>
    public async Task<List<AgentRecord>> GetRegisteredAgentsAsync()
    {
        var page = await _httpClient.GetFromJsonAsync<AgentPageResponse>(
            $"{_baseUrl}/api/agents?page=1&pageSize=100");

        return page?.Items ?? new List<AgentRecord>();
    }
}
