package com.thrillhouse.suppression;

/** Result of publishing a {@link SyncStats} snapshot to the metrics backend. */
public enum PublishOutcome {
    /** The metrics backend accepted the snapshot. */
    SUCCESS,
    /** The metrics backend rejected the snapshot or was unreachable. */
    FAILED
}
