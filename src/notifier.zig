const std = @import("std");
const Allocator = std.mem.Allocator;

pub const max_summary_bytes: usize = 4000;

/// Posts the run summary to the ops notification webhook. The receiving
/// channel rejects payloads larger than `max_summary_bytes`, so callers
/// that build large summaries (an org with many projects can easily
/// exceed it) must trim the text themselves before calling this --
/// sendSummary returns error.PayloadTooLarge rather than truncating on
/// the caller's behalf.
pub fn sendSummary(allocator: Allocator, webhook_url: []const u8, text: []const u8) !void {
    if (text.len > max_summary_bytes) return error.PayloadTooLarge;

    const body = try std.fmt.allocPrint(allocator, "{{\"text\":\"{s}\"}}", .{text});
    defer allocator.free(body);

    var client = std.http.Client{ .allocator = allocator };
    defer client.deinit();

    var response_body = std.ArrayList(u8).init(allocator);
    defer response_body.deinit();

    const result = try client.fetch(.{
        .location = .{ .url = webhook_url },
        .method = .POST,
        .payload = body,
        .response_storage = .{ .dynamic = &response_body },
    });
    if (result.status != .ok) return error.NotifyFailed;
}
