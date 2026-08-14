const std = @import("std");

/// A plain token bucket guarding the alert channel, which drops a sender that
/// posts more than `burst` messages between refills.
pub const TokenBucket = struct {
    available: u32,
    burst: u32,

    pub fn init(burst: u32) TokenBucket {
        return .{ .available = burst, .burst = burst };
    }

    /// Takes up to `want` tokens from the bucket and returns how many were
    /// actually granted. The grant is between 0 and `want`: a bucket that is
    /// short hands back what it has, and a drained bucket returns 0. Callers
    /// must send only the granted number of messages and carry the remainder
    /// over to a later refill.
    pub fn reserve(self: *TokenBucket, want: u32) u32 {
        const granted = @min(want, self.available);
        self.available -= granted;
        return granted;
    }

    /// Restores the bucket to its full burst allowance.
    pub fn refill(self: *TokenBucket) void {
        self.available = self.burst;
    }
};
