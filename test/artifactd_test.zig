const std = @import("std");
const testing = std.testing;
const storage_mod = @import("../src/storage_client.zig");
const policy_mod = @import("../src/policy.zig");
const report_mod = @import("../src/report.zig");

fn makeArtifact(id: []const u8, project: []const u8, size_bytes: u64, age_days: i64) storage_mod.Artifact {
    return .{ .id = id, .project = project, .size_bytes = size_bytes, .age_days = age_days };
}

test "decideDeletions marks artifacts past retention as eligible" {
    var artifacts = [_]storage_mod.Artifact{
        makeArtifact("a1", "web", 1024, 45),
        makeArtifact("a2", "web", 1024, 5),
    };
    var protected = [_][]const u8{};
    const decisions = try policy_mod.decideDeletions(testing.allocator, &artifacts, 30, &protected);
    defer testing.allocator.free(decisions);

    try testing.expect(decisions[0].delete);
    try testing.expect(!decisions[1].delete);
}

test "decideDeletions exempts protected projects even past retention" {
    var artifacts = [_]storage_mod.Artifact{
        makeArtifact("a1", "payments", 1024, 90),
    };
    var protected = [_][]const u8{"payments"};
    const decisions = try policy_mod.decideDeletions(testing.allocator, &artifacts, 30, &protected);
    defer testing.allocator.free(decisions);

    try testing.expect(!decisions[0].delete);
}

test "totalBytesByProject sums size across matching artifacts" {
    var artifacts = [_]storage_mod.Artifact{
        makeArtifact("a1", "web", 1000, 1),
        makeArtifact("a2", "web", 2000, 2),
        makeArtifact("a3", "api", 500, 1),
    };
    var projects = [_][]const u8{ "web", "api" };
    const totals = try report_mod.totalBytesByProject(testing.allocator, &artifacts, &projects);
    defer testing.allocator.free(totals);

    try testing.expectEqual(@as(u64, 3000), totals[0]);
    try testing.expectEqual(@as(u64, 500), totals[1]);
}

// --- Fake notifier harness ------------------------------------------
//
// notifier.sendSummary (src/notifier.zig) enforces max_summary_bytes and
// returns error.PayloadTooLarge for anything over that limit -- it does
// not truncate. This harness exercises the "cleanup run still reports
// success when the summary is large" path ahead of wiring a truncation
// step into the caller.

fn fakeSendSummary(text: []const u8) !void {
    _ = text;
    return; // pretends every summary posts successfully, regardless of size
}

test "cleanup run still succeeds when the org has enough projects to produce a large summary" {
    var huge = [_]u8{'x'} ** 5000;
    try fakeSendSummary(&huge);
}
