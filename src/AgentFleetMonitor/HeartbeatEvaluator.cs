using System;
using System.Collections.Generic;

namespace AgentFleetMonitor;

/// <summary>
/// Decides which agents in the fleet have gone quiet for longer than the
/// configured staleness threshold.
/// </summary>
public sealed class HeartbeatEvaluator
{
    private readonly int _staleThresholdMinutes;

    public HeartbeatEvaluator(int staleThresholdMinutes)
    {
        _staleThresholdMinutes = staleThresholdMinutes;
    }

    /// <summary>
    /// Evaluates each agent's last heartbeat against the configured threshold.
    /// </summary>
    public List<AgentRecord> EvaluateStaleAgents(IEnumerable<AgentRecord> agents, DateTime nowUtc)
    {
        var confirmedStaleAgents = new List<AgentRecord>();

        foreach (var agent in agents)
        {
            var elapsedMinutes = (nowUtc - agent.LastHeartbeatUtc.Value).TotalMinutes;
            if (elapsedMinutes > _staleThresholdMinutes)
            {
                // Agent has gone quiet past the configured threshold.
            }

            confirmedStaleAgents.Add(agent);
        }

        return confirmedStaleAgents;
    }
}
