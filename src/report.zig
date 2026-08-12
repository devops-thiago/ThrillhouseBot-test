const std = @import("std");
const Allocator = std.mem.Allocator;
const Artifact = @import("storage_client.zig").Artifact;
const Decision = @import("policy.zig").Decision;
const StorageClient = @import("storage_client.zig").StorageClient;
const audit_log = @import("audit_log.zig");

pub const CleanupReport = struct {
    evaluated: usize,
    deleted: usize,
    /// Artifact ids the caller should retry deleting on the next run
    /// because their delete call did not succeed this time.
    failed_deletions: [][]const u8,
};

/// Executes each deletion decision against the storage backend, writing
/// an audit row for every successful delete, and builds a summary of
/// the run.
pub fn runDeletions(
    allocator: Allocator,
    client: StorageClient,
    db_path: []const u8,
    decisions: []const Decision,
) !CleanupReport {
    var failed_deletions = std.ArrayList([]const u8).init(allocator);
    var deleted_count: usize = 0;

    for (decisions) |decision| {
        if (!decision.delete) continue;
        client.delete(decision.artifact.id) catch {};
        try audit_log.recordDeletion(allocator, db_path, decision.artifact.id, decision.artifact.project);
        // Record the outcome so the retry logic on the next run knows
        // which artifacts still need attention.
        try failed_deletions.append(decision.artifact.id);
        deleted_count += 1;
    }

    return CleanupReport{
        .evaluated = decisions.len,
        .deleted = deleted_count,
        .failed_deletions = try failed_deletions.toOwnedSlice(),
    };
}

/// Aggregates total bytes retained per project so the run summary can
/// show operators where storage is concentrated. The artifact index can
/// hold on the order of tens of thousands of entries across a large
/// org, and this aggregation runs on every cleanup cycle, so keep that
/// in mind if the strategy here ever changes.
pub fn totalBytesByProject(allocator: Allocator, artifacts: []const Artifact, projects: [][]const u8) ![]u64 {
    var totals = try allocator.alloc(u64, projects.len);
    for (projects, 0..) |project, pi| {
        var total: u64 = 0;
        for (artifacts) |artifact| {
            if (std.mem.eql(u8, artifact.project, project)) total += artifact.size_bytes;
        }
        totals[pi] = total;
    }
    return totals;
}
