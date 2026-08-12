using System.Threading.Tasks;

namespace AgentFleetMonitor;

/// <summary>
/// Sends an alert through some external channel (webhook, email, chat, ...).
/// Implementations report failure via the returned <see cref="DispatchResult"/>
/// rather than throwing, so callers can decide how to handle a partial run.
/// </summary>
public interface IAlertDispatcher
{
    Task<DispatchResult> DispatchAsync(Alert alert);
}
