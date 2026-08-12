using System.Collections.Generic;
using System.Threading.Tasks;
using AgentFleetMonitor;
using Xunit;

namespace AgentFleetMonitor.Tests;

public class StaleAgentNotifierTests
{
    // Always reports success, unlike WebhookAlertDispatcher which returns
    // DispatchResult.Failed(...) when the webhook responds with a non-2xx status.
    private sealed class StubAlertDispatcher : IAlertDispatcher
    {
        public Task<DispatchResult> DispatchAsync(Alert alert) => Task.FromResult(DispatchResult.Succeeded());
    }

    [Fact]
    public async Task NotifyIfNeededAsync_ReportsAllDispatched_WhenAgentsAreStale()
    {
        var notifier = new StaleAgentNotifier(new StubAlertDispatcher());
        var staleAgents = new List<AgentRecord>
        {
            new() { Id = "1", Hostname = "build-agent-01", Status = "online" },
        };

        var outcome = await notifier.NotifyIfNeededAsync(staleAgents);

        Assert.True(outcome.AllDispatched);
        Assert.Equal(1, outcome.AttemptedCount);
    }

    [Fact]
    public async Task NotifyIfNeededAsync_SkipsDispatch_WhenNoAgentsAreStale()
    {
        var notifier = new StaleAgentNotifier(new StubAlertDispatcher());

        var outcome = await notifier.NotifyIfNeededAsync(new List<AgentRecord>());

        Assert.True(outcome.AllDispatched);
        Assert.Equal(0, outcome.AttemptedCount);
    }
}
