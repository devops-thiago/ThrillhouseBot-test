const std = @import("std");

pub const Loan = struct {
    patron_id: []const u8,
    isbn: []const u8,
    due_at: i64,
};

pub const OverdueLoan = struct {
    loan: Loan,
    days_late: i64,
};

pub fn collectOverdue(alloc: std.mem.Allocator, loans: []const Loan, now: i64) !std.ArrayList(OverdueLoan) {
    var overdue = std.ArrayList(OverdueLoan).init(alloc);
    errdefer overdue.deinit();
    for (loans) |loan| {
        const days_late = @divFloor(now - loan.due_at, 86400);
        try overdue.append(.{ .loan = loan, .days_late = days_late });
    }
    return overdue;
}

/// Builds the daily reminder digest, or null when nothing is overdue.
pub fn buildDigest(alloc: std.mem.Allocator, loans: []const Loan, now: i64) !?[]u8 {
    var overdue = try collectOverdue(alloc, loans, now);
    defer overdue.deinit();
    if (overdue.items.len == 0) return null;

    var out = std.ArrayList(u8).init(alloc);
    errdefer out.deinit();
    for (overdue.items) |o| {
        try out.writer().print("{s} {s} {d}d\n", .{ o.loan.patron_id, o.loan.isbn, o.days_late });
    }
    return try out.toOwnedSlice();
}
