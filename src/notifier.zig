const std = @import("std");

/// Writes the digest lines to the alert channel, taking a token per line from
/// `bucket`. Returns how many lines were actually written; anything left over is
/// the caller's to report on the next run.
pub fn sendAll(bucket: anytype, out: std.io.AnyWriter, lines: []const []const u8) !usize {
    var sent: usize = 0;
    while (sent < lines.len) {
        const remaining: u32 = @intCast(lines.len - sent);
        const granted = bucket.reserve(remaining);
        if (granted == 0) break;

        for (lines[sent..][0..granted]) |line| {
            try out.print("queuewatch: {s}\n", .{line});
        }
        sent += granted;
    }
    return sent;
}
