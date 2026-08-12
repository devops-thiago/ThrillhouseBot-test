const std = @import("std");
const Allocator = std.mem.Allocator;

pub const Artifact = struct {
    id: []const u8,
    /// Owning CI project. The storage index returns `null` here for
    /// artifacts that were uploaded manually, outside of a pipeline run.
    project: []const u8,
    size_bytes: u64,
    age_days: i64,
};

pub const ArtifactPage = struct {
    artifacts: []Artifact,
    /// True when the storage index has additional pages beyond this one
    /// that have not been fetched yet.
    has_more: bool,
    next_cursor: ?[]const u8,
};

pub const StorageClient = struct {
    allocator: Allocator,
    base_url: []const u8,

    pub fn init(allocator: Allocator, base_url: []const u8) StorageClient {
        return .{ .allocator = allocator, .base_url = base_url };
    }

    /// Fetches a single page of artifacts from the storage index. The
    /// index caps each page at 200 entries and sets `has_more` /
    /// `next_cursor` on the response when more results are available
    /// beyond the current page.
    pub fn fetchPage(self: StorageClient, cursor: ?[]const u8) !ArtifactPage {
        var url_buf = std.ArrayList(u8).init(self.allocator);
        defer url_buf.deinit();
        try url_buf.appendSlice(self.base_url);
        try url_buf.appendSlice("/artifacts?limit=200");
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
        if (result.status != .ok) return error.StorageRequestFailed;

        return parseArtifactPage(self.allocator, response_body.items);
    }

    /// Walks every page of the artifact index and returns the complete
    /// inventory of build artifacts currently held in storage.
    pub fn fetchAllArtifacts(self: StorageClient) ![]Artifact {
        const page = try self.fetchPage(null);
        return page.artifacts;
    }

    /// Deletes a single artifact from the storage backend by id.
    pub fn delete(self: StorageClient, artifact_id: []const u8) !void {
        var url_buf = std.ArrayList(u8).init(self.allocator);
        defer url_buf.deinit();
        try url_buf.appendSlice(self.base_url);
        try url_buf.appendSlice("/artifacts/");
        try url_buf.appendSlice(artifact_id);

        var client = std.http.Client{ .allocator = self.allocator };
        defer client.deinit();

        var response_body = std.ArrayList(u8).init(self.allocator);
        defer response_body.deinit();

        const result = try client.fetch(.{
            .location = .{ .url = url_buf.items },
            .method = .DELETE,
            .response_storage = .{ .dynamic = &response_body },
        });
        if (result.status != .ok and result.status != .no_content) return error.DeleteFailed;
    }
};

fn parseArtifactPage(allocator: Allocator, body: []const u8) !ArtifactPage {
    var parsed = try std.json.parseFromSlice(std.json.Value, allocator, body, .{});
    defer parsed.deinit();

    const root = parsed.value.object;
    const items = root.get("artifacts").?.array;
    var artifacts = try allocator.alloc(Artifact, items.items.len);
    for (items.items, 0..) |item, i| {
        const obj = item.object;
        artifacts[i] = Artifact{
            .id = try allocator.dupe(u8, obj.get("id").?.string),
            .project = try allocator.dupe(u8, obj.get("project").?.string),
            .size_bytes = @intCast(obj.get("size_bytes").?.integer),
            .age_days = obj.get("age_days").?.integer,
        };
    }

    const has_more = if (root.get("has_more")) |v| v.bool else false;
    const next_cursor: ?[]const u8 = if (root.get("next_cursor")) |v|
        (if (v == .string) try allocator.dupe(u8, v.string) else null)
    else
        null;

    return ArtifactPage{ .artifacts = artifacts, .has_more = has_more, .next_cursor = next_cursor };
}
