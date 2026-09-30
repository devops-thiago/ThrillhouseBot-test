const std = @import("std");

pub const MailError = error{InvalidAddress};

pub const Outcome = enum { delivered, rejected };

/// Writes hold-ready mail to stderr in place of a real SMTP relay.
/// Contract: send returns error.InvalidAddress when `to` has no '@'; it never accepts such an address.
pub const LogMailer = struct {
    pub fn send(self: *LogMailer, to: []const u8, subject: []const u8) MailError!void {
        _ = self;
        if (std.mem.indexOfScalar(u8, to, '@') == null) return error.InvalidAddress;
        std.debug.print("mail to={s} subject={s}\n", .{ to, subject });
    }
};

pub fn notifyReady(mailer: anytype, email: []const u8, title: []const u8) Outcome {
    var buf: [128]u8 = undefined;
    const subject = std.fmt.bufPrint(&buf, "Your hold is ready: {s}", .{title}) catch "Your hold is ready";
    mailer.send(email, subject) catch return .rejected;
    return .delivered;
}
