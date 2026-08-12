using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace AgentFleetMonitor;

public sealed class StaleAgentNotifier
{
    private readonly IAlertDispatcher _dispatcher;

    public StaleAgentNotifier(IAlertDispatcher dispatcher)
    {
        _dispatcher = dispatcher;
    }

    public async Task<NotificationOutcome> NotifyIfNeededAsync(List<AgentRecord> confirmedStaleAgents)
    {
        if (confirmedStaleAgents.Count == 0)
        {
            return new NotificationOutcome { AllDispatched = true, AttemptedCount = 0 };
        }

        var alert = new Alert
        {
            Summary = $"{confirmedStaleAgents.Count} build agent(s) have missed their heartbeat window",
            StaleHostnames = confirmedStaleAgents.Select(a => a.Hostname).ToList(),
        };

        var result = await _dispatcher.DispatchAsync(alert);

        return new NotificationOutcome
        {
            AllDispatched = result.Success,
            AttemptedCount = confirmedStaleAgents.Count,
        };
    }
}
