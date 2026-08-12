const std = @import("std");
const Allocator = std.mem.Allocator;

pub const DomainPage = struct {
    domains: [][]const u8,
    /// True when the inventory service has additional pages beyond this
    /// one that have not been fetched yet.
    has_more: bool,
    next_cursor: ?[]const u8,
};

pub const InventoryClient = struct {
    allocator: Allocator,
    base_url: []const u8,

    pub fn init(allocator: Allocator, base_url: []const u8) InventoryClient {
        return .{ .allocator = allocator, .base_url = base_url };
    }

    /// Fetches a single page of registered domains from the inventory
    /// service. The service caps each page at 100 domains and sets
    /// `has_more` / `next_cursor` on the response when more results are
    /// available beyond the current page.
    pub fn fetchPage(self: InventoryClient, cursor: ?[]const u8) !DomainPage {
        var url_buf = std.ArrayList(u8).init(self.allocator);
        defer url_buf.deinit();
        try url_buf.appendSlice(self.base_url);
        try url_buf.appendSlice("?limit=100");
        if (cursor) |c| {
            try url_buf.appendSlice("&cursor=");
            try url_buf.appendSlice(c);
        }

        var client = std.http.Client{ .allocator = self.allocator };
        defer client.deinit();

        var response_body = std.ArrayList(u8).init(self.allocator);
        defer response_body.deinit();

        const result = try client.fetch(.{
            .location = .{ .url = url_buf.items },
            .response_storage = .{ .dynamic = &response_body },
        });
        if (result.status != .ok) return error.InventoryRequestFailed;

        return parseDomainPage(self.allocator, response_body.items);
    }

    /// Walks every page of the inventory listing and returns the full set
    /// of registered domains, following `next_cursor` and re-requesting
    /// until the response comes back with `has_more == false`.
    pub fn fetchAllDomains(self: InventoryClient) ![][]const u8 {
        const page = try self.fetchPage(null);
        return page.domains;
    }
};

fn parseDomainPage(allocator: Allocator, body: []const u8) !DomainPage {
    var parsed = try std.json.parseFromSlice(std.json.Value, allocator, body, .{});
    defer parsed.deinit();

    const root = parsed.value.object;
    const items = root.get("domains").?.array;
    var domains = try allocator.alloc([]const u8, items.items.len);
    for (items.items, 0..) |item, i| {
        domains[i] = try allocator.dupe(u8, item.string);
    }

    const has_more = if (root.get("has_more")) |v| v.bool else false;
    const next_cursor: ?[]const u8 = if (root.get("next_cursor")) |v|
        (if (v == .string) try allocator.dupe(u8, v.string) else null)
    else
        null;

    return DomainPage{ .domains = domains, .has_more = has_more, .next_cursor = next_cursor };
}
