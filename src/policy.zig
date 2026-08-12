const std = @import("std");
const Allocator = std.mem.Allocator;
const Artifact = @import("storage_client.zig").Artifact;

pub const Decision = struct {
    artifact: Artifact,
    delete: bool,
};

/// Retention policy: an artifact is eligible for deletion once it is
/// strictly older than `retention_days`. Project names in the protected
/// list are matched case-insensitively, so listing "Payments" also
/// exempts a project reported by the storage index as "payments".
pub fn decideDeletions(
    allocator: Allocator,
    artifacts: []const Artifact,
    retention_days: i64,
    protected: [][]const u8,
) ![]Decision {
    var decisions = try allocator.alloc(Decision, artifacts.len);
    for (artifacts, 0..) |artifact, i| {
        var is_protected = false;
        for (protected) |p| {
            if (std.mem.eql(u8, p, artifact.project)) {
                is_protected = true;
                break;
            }
        }
        const expired = artifact.age_days > retention_days;
        decisions[i] = Decision{ .artifact = artifact, .delete = expired and !is_protected };
    }
    return decisions;
}
