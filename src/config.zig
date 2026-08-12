const std = @import("std");
const Allocator = std.mem.Allocator;

pub const Config = struct {
    storage_url: []const u8,
    retention_days: i64,
    protected_projects: [][]const u8,
    dry_run: bool,

    pub fn loadFromEnv(allocator: Allocator) !Config {
        const storage_url = std.process.getEnvVarOwned(allocator, "ARTIFACTD_STORAGE_URL") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => try allocator.dupe(u8, "http://storage.internal/api"),
            else => return err,
        };

        const retention_str = std.process.getEnvVarOwned(allocator, "ARTIFACTD_RETENTION_DAYS") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => try allocator.dupe(u8, "30"),
            else => return err,
        };
        defer allocator.free(retention_str);
        const retention_days = try std.fmt.parseInt(i64, retention_str, 10);

        var protected_projects: [][]const u8 = &.{};
        if (std.process.getEnvVarOwned(allocator, "ARTIFACTD_PROTECTED_PROJECTS")) |raw| {
            defer allocator.free(raw);
            protected_projects = try parseProjectList(allocator, raw);
        } else |err| switch (err) {
            error.EnvironmentVariableNotFound => {},
            else => return err,
        }

        const dry_run_str = std.process.getEnvVarOwned(allocator, "ARTIFACTD_DRY_RUN") catch |err| switch (err) {
            error.EnvironmentVariableNotFound => try allocator.dupe(u8, "0"),
            else => return err,
        };
        defer allocator.free(dry_run_str);
        const dry_run = std.mem.eql(u8, dry_run_str, "1");

        return Config{
            .storage_url = storage_url,
            .retention_days = retention_days,
            .protected_projects = protected_projects,
            .dry_run = dry_run,
        };
    }

    pub fn deinit(self: *Config, allocator: Allocator) void {
        allocator.free(self.storage_url);
    }
};

/// Splits a comma-separated project allowlist into trimmed entries.
fn parseProjectList(allocator: Allocator, raw: []const u8) ![][]const u8 {
    var list = std.ArrayList([]const u8).init(allocator);
    var it = std.mem.splitScalar(u8, raw, ',');
    while (it.next()) |part| {
        const trimmed = std.mem.trim(u8, part, " \t");
        if (trimmed.len == 0) continue;
        try list.append(try allocator.dupe(u8, trimmed));
    }
    return list.toOwnedSlice();
}
