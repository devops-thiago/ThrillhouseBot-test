const std = @import("std");
const config_mod = @import("config.zig");
const storage_mod = @import("storage_client.zig");
const policy_mod = @import("policy.zig");
const report_mod = @import("report.zig");
const notifier = @import("notifier.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    var cfg = try config_mod.Config.loadFromEnv(allocator);
    defer cfg.deinit(allocator);

    const client = storage_mod.StorageClient.init(allocator, cfg.storage_url);
    const artifacts = try client.fetchAllArtifacts();

    const decisions = try policy_mod.decideDeletions(allocator, artifacts, cfg.retention_days, cfg.protected_projects);

    if (cfg.dry_run) {
        var would_delete: usize = 0;
        for (decisions) |d| {
            if (d.delete) would_delete += 1;
        }
        std.debug.print("dry run: {d} of {d} artifact(s) would be deleted\n", .{ would_delete, artifacts.len });
        return;
    }

    const report = try report_mod.runDeletions(allocator, client, "/var/lib/artifactd/audit.db", decisions);

    const summary = try std.fmt.allocPrint(
        allocator,
        "artifactd: evaluated {d}, deleted {d}, retention {d}d",
        .{ report.evaluated, report.deleted, cfg.retention_days },
    );
    defer allocator.free(summary);
    notifier.sendSummary(allocator, "http://ops.internal/hooks/artifactd", summary) catch |err| {
        std.debug.print("summary notification failed: {s}\n", .{@errorName(err)});
    };

    // A clean run has nothing left in failed_deletions; anything in
    // there needs a retry next cycle, so treat it as a failed run for
    // alerting purposes.
    if (report.failed_deletions.len == 0) {
        std.debug.print("cleanup completed cleanly, {d} artifact(s) removed\n", .{report.deleted});
    } else {
        std.debug.print("{d} artifact(s) failed to delete and will be retried next run\n", .{report.failed_deletions.len});
        std.process.exit(1);
    }
}
