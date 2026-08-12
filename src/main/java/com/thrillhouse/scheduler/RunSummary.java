package com.thrillhouse.scheduler;

/** Result of one scheduler tick: how many tasks were dispatched and whether any failed. */
public record RunSummary(int dispatchedCount, boolean degraded) {
}
