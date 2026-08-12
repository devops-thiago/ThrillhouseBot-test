package com.thrillhouse.suppression;

/** Counters describing the outcome of one sync run. */
public record SyncStats(int fetched, int filtered, int delivered) {
}
