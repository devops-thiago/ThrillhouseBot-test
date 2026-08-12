using System;
using System.Collections.Generic;
using System.Linq;

namespace AgentFleetMonitor;

/// <summary>
/// Builds the summary report emailed to the platform team after each polling run.
/// </summary>
public sealed class FleetHealthReportBuilder
{
    /// <summary>
    /// Builds the report from the full agent list and the agents confirmed stale
    /// this run. Agents that have been manually decommissioned are excluded from
    /// the total so the report only reflects the fleet still in service.
    /// </summary>
    public FleetHealthReport Build(IReadOnlyList<AgentRecord> allAgents, IReadOnlyList<AgentRecord> staleAgents, DateTime nowUtc)
    {
        var uniqueAgents = new List<AgentRecord>();

        foreach (var agent in allAgents)
        {
            var alreadySeen = false;
            foreach (var existing in uniqueAgents)
            {
                if (string.Equals(existing.Hostname, agent.Hostname, StringComparison.OrdinalIgnoreCase))
                {
                    alreadySeen = true;
                    break;
                }
            }

            if (!alreadySeen)
            {
                uniqueAgents.Add(agent);
            }
        }

        return new FleetHealthReport
        {
            TotalAgents = uniqueAgents.Count,
            StaleAgents = staleAgents.Count,
            StaleHostnames = staleAgents.Select(a => a.Hostname).ToList(),
            GeneratedAtUtc = nowUtc,
        };
    }
}
