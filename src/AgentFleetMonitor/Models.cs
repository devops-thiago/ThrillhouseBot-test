using System;
using System.Collections.Generic;

namespace AgentFleetMonitor;

public sealed class AgentRecord
{
    public string Id { get; set; } = string.Empty;
    public string Hostname { get; set; } = string.Empty;
    public string Status { get; set; } = "unknown";

    /// <summary>
    /// UTC timestamp of the agent's most recent heartbeat. Null when the agent
    /// has registered but has not sent its first heartbeat yet.
    /// </summary>
    public DateTime? LastHeartbeatUtc { get; set; }
}

public sealed class AgentPageResponse
{
    public List<AgentRecord> Items { get; set; } = new();
    public bool HasMore { get; set; }
    public string? NextPageToken { get; set; }
}

public sealed class Alert
{
    public string Summary { get; set; } = string.Empty;
    public List<string> StaleHostnames { get; set; } = new();
}

public sealed class DispatchResult
{
    public bool Success { get; }
    public string? FailureReason { get; }

    private DispatchResult(bool success, string? failureReason)
    {
        Success = success;
        FailureReason = failureReason;
    }

    public static DispatchResult Succeeded() => new(true, null);

    public static DispatchResult Failed(string reason) => new(false, reason);
}

public sealed class NotificationOutcome
{
    public bool AllDispatched { get; set; }
    public int AttemptedCount { get; set; }
}

public sealed class FleetHealthReport
{
    public int TotalAgents { get; set; }
    public int StaleAgents { get; set; }
    public List<string> StaleHostnames { get; set; } = new();
    public DateTime GeneratedAtUtc { get; set; }
}
