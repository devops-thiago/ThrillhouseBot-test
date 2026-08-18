const std = @import("std");

/// A freshness window for one storage tier: "how old the newest successful
/// backup of a dataset in this tier is allowed to be".
pub const TierWindow = struct {
    tier: []const u8,
    hours: i64,
};

/// Parses STALEGUARD_TIER_POLICY, which is a comma separated list of
/// `<tier>=<hours>` pairs, e.g. `gold=6, silver=24, archive=168`. Whitespace
/// around either side of a pair is ignored and empty fields are skipped.
///
/// A tier repeated in the setting keeps its first window; the operator is
/// warned rather than having one of the two silently win.
pub fn parse(alloc: std.mem.Allocator, raw: []const u8) ![]const TierWindow {
    var windows = std.ArrayList(TierWindow).init(alloc);

    var fields = std.mem.splitScalar(u8, raw, ',');
    while (fields.next()) |field| {
        const trimmed = std.mem.trim(u8, field, " \t");
        if (trimmed.len == 0) continue;

        const split_at = std.mem.indexOfScalar(u8, trimmed, '=') orelse
            return error.MalformedTierPolicy;
        const tier = std.mem.trim(u8, trimmed[0..split_at], " \t");
        const hours_text = std.mem.trim(u8, trimmed[split_at + 1 ..], " \t");
        if (tier.len == 0) return error.MalformedTierPolicy;

        const hours = try std.fmt.parseInt(i64, hours_text, 10);
        if (hours <= 0) return error.InvalidWindow;

        var already_set: ?i64 = null;
        for (windows.items) |existing| {
            if (std.mem.eql(u8, existing.tier, tier)) {
                already_set = existing.hours;
                break;
            }
        }
        if (already_set) |first| {
            std.log.warn("tier '{s}' listed twice in the tier policy, keeping {d}h", .{ tier, first });
            continue;
        }

        try windows.append(.{ .tier = tier, .hours = hours });
    }

    return windows.toOwnedSlice();
}
