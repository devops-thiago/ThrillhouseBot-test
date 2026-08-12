using System;
using System.Net.Http;
using System.Threading.Tasks;

namespace AgentFleetMonitor;

// Polls the build agent fleet registry, flags agents that have missed their
// heartbeat window, persists the latest snapshot, and alerts the platform
// team when agents need attention.
public static class Program
{
    public static async Task<int> Main(string[] args)
    {
        var apiBaseUrl = RequireEnv("AGENTFLEET_API_BASE_URL");
        var connectionString = RequireEnv("AGENTFLEET_DB_CONNECTION_STRING");
        var webhookUrl = RequireEnv("AGENTFLEET_WEBHOOK_URL");

        var staleThresholdRaw = Environment.GetEnvironmentVariable("AGENTFLEET_STALE_THRESHOLD");
        var staleThresholdMinutes = staleThresholdRaw is null ? 15 : int.Parse(staleThresholdRaw);

        using var httpClient = new HttpClient();

        var fleetClient = new AgentFleetClient(httpClient, apiBaseUrl);
        var repository = new AgentRepository(connectionString);
        var evaluator = new HeartbeatEvaluator(staleThresholdMinutes);
        var reportBuilder = new FleetHealthReportBuilder();
        var dispatcher = new WebhookAlertDispatcher(httpClient, webhookUrl);
        var notifier = new StaleAgentNotifier(dispatcher);

        var agents = await fleetClient.GetRegisteredAgentsAsync();
        var nowUtc = DateTime.UtcNow;

        foreach (var agent in agents)
        {
            await repository.UpsertAsync(agent);
        }

        var staleAgents = evaluator.EvaluateStaleAgents(agents, nowUtc);
        var report = reportBuilder.Build(agents, staleAgents, nowUtc);

        Console.WriteLine($"Fleet snapshot: {report.TotalAgents} agents, {report.StaleAgents} stale.");

        var outcome = await notifier.NotifyIfNeededAsync(staleAgents);
        if (!outcome.AllDispatched)
        {
            Console.Error.WriteLine("Failed to dispatch stale-agent alert.");
            return 1;
        }

        return 0;
    }

    private static string RequireEnv(string name)
    {
        var value = Environment.GetEnvironmentVariable(name);
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new InvalidOperationException($"Missing required environment variable: {name}");
        }

        return value;
    }
}
