using System.Collections.Generic;
using System.Threading.Tasks;

namespace AgentFleetMonitor;

/// <summary>
/// Talks to the fleet registry API that tracks build agents and their heartbeats.
/// </summary>
public interface IAgentFleetClient
{
    Task<List<AgentRecord>> GetRegisteredAgentsAsync();
}
