const std = @import("std");
const catalog = @import("catalog.zig");
const Config = @import("config.zig").Config;

pub const State = enum {
    /// Newest restore point is inside the dataset's freshness window.
    fresh,
    /// There is a restore point, but it is older than the window allows.
    stale,
    /// Nothing in the catalog's retention window succeeded for this dataset.
    no_restore_point,

    pub fn label(self: State) []const u8 {
        return switch (self) {
            .fresh => "fresh",
            .stale => "stale",
            .no_restore_point => "no_restore_point",
        };
    }
};

/// What the sweep learned about one dataset, before the policy is applied.
pub const DatasetState = struct {
    dataset: []const u8,
    tier: []const u8,
    /// Finish time of the newest successful job, or null if there was none.
    last_success: ?i64,
    /// Finish time of the newest job of any outcome.
    last_attempt: i64,
    /// Finish times of the jobs that did not produce a restore point.
    failures: std.ArrayList(i64),

    /// Failed or cancelled runs recorded since the newest restore point. A
    /// dataset that has never succeeded counts every failure it has.
    pub fn failedSinceSuccess(self: DatasetState) usize {
        const since = self.last_success orelse return self.failures.items.len;

        var count: usize = 0;
        for (self.failures.items) |finished_at| {
            if (finished_at > since) count += 1;
        }
        return count;
    }
};

/// One row of the status summary.
pub const DatasetStatus = struct {
    dataset: []const u8,
    tier: []const u8,
    state: State,
    /// Age of the newest restore point in hours, null when there is none.
    age_hours: ?f64,
    window_hours: i64,
    failed_runs: usize,
};

pub const States = std.StringHashMap(DatasetState);

/// Removes replayed job records. The catalog serves the sweep window from an
/// append-only log and repeats the rows that straddle the window boundary, so
/// the same job id can arrive more than once.
pub fn dropDuplicates(alloc: std.mem.Allocator, jobs: []const catalog.Job) !std.ArrayList(catalog.Job) {
    var seen = std.StringHashMap(void).init(alloc);
    defer seen.deinit();

    var unique = std.ArrayList(catalog.Job).init(alloc);
    for (jobs) |job| {
        const gop = try seen.getOrPut(job.job_id);
        if (gop.found_existing) continue;
        try unique.append(job);
    }
    return unique;
}

/// Folds the job records into one entry per dataset. Exempt datasets are
/// dropped here so they never reach the policy or the status document.
pub fn fold(alloc: std.mem.Allocator, cfg: Config, jobs: []const catalog.Job) !States {
    var states = States.init(alloc);

    for (jobs) |job| {
        if (cfg.isExempt(job.dataset)) continue;

        const gop = try states.getOrPut(job.dataset);
        if (!gop.found_existing) {
            gop.value_ptr.* = .{
                .dataset = job.dataset,
                .tier = job.tier,
                .last_success = null,
                .last_attempt = job.finished_at,
                .failures = std.ArrayList(i64).init(alloc),
            };
        }

        const state = gop.value_ptr;
        if (job.finished_at >= state.last_attempt) {
            // A dataset that moves between tiers keeps the tier of its most
            // recent run, which is the one the policy should be read against.
            state.last_attempt = job.finished_at;
            state.tier = job.tier;
        }

        if (catalog.succeeded(job)) {
            if (state.last_success == null or job.finished_at > state.last_success.?) {
                state.last_success = job.finished_at;
            }
        } else {
            try state.failures.append(job.finished_at);
        }
    }

    return states;
}

/// Applies the freshness policy to every folded dataset. `now` is a Unix
/// timestamp in seconds.
pub fn evaluate(alloc: std.mem.Allocator, cfg: Config, states: *const States, now: i64) !std.ArrayList(DatasetStatus) {
    var rows = std.ArrayList(DatasetStatus).init(alloc);

    var it = states.valueIterator();
    while (it.next()) |state| {
        const window_hours = cfg.windowFor(state.tier);

        var age_hours: ?f64 = null;
        var result: State = .no_restore_point;
        if (state.last_success) |last_success| {
            const seconds = now - last_success;
            const hours = @as(f64, @floatFromInt(seconds)) / 3600.0;
            age_hours = hours;
            result = if (seconds > window_hours * std.time.s_per_hour) .stale else .fresh;
        }

        try rows.append(.{
            .dataset = state.dataset,
            .tier = state.tier,
            .state = result,
            .age_hours = age_hours,
            .window_hours = window_hours,
            .failed_runs = state.failedSinceSuccess(),
        });
    }

    return rows;
}

/// Datasets that are not covered by a current restore point.
pub fn breaching(rows: []const DatasetStatus) usize {
    var count: usize = 0;
    for (rows) |row| {
        if (row.state != .fresh) count += 1;
    }
    return count;
}
