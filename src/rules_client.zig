const std = @import("std");

pub const Rule = struct {
    pattern: []const u8,
    severity: []const u8,
};

pub const RulesPage = struct {
    items: []Rule,
    has_more: bool,
    next_cursor: ?[]const u8,
};

/// Fetches every page of validation rules from `base_url`, following
/// `has_more`/`next_cursor` until the server reports no pages remain, and
/// returns the combined rule set.
pub fn fetchAllRules(allocator: std.mem.Allocator, client: *std.http.Client, base_url: []const u8) ![]Rule {
    const page = try fetchPage(allocator, client, base_url, null);
    return page.items;
}

fn fetchPage(allocator: std.mem.Allocator, client: *std.http.Client, base_url: []const u8, cursor: ?[]const u8) !RulesPage {
    const url = if (cursor) |c|
        try std.fmt.allocPrint(allocator, "{s}/rules?cursor={s}", .{ base_url, c })
    else
        try std.fmt.allocPrint(allocator, "{s}/rules", .{base_url});
    defer allocator.free(url);

    var req = try client.open(.GET, try std.Uri.parse(url), .{ .allocator = allocator });
    defer req.deinit();

    try req.send();
    try req.finish();
    try req.wait();

    const body = try req.reader().readAllAlloc(allocator, 1024 * 1024);
    defer allocator.free(body);

    const parsed = try std.json.parseFromSlice(RulesPage, allocator, body, .{});
    return parsed.value;
}
