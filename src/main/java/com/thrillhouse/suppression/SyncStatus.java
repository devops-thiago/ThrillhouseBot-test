package com.thrillhouse.suppression;

/** Overall outcome of a campaign sync run. */
public enum SyncStatus {
    /** Every recipient validated cleanly and was handed off for delivery. */
    SUCCESS,
    /** Some recipients failed validation and were excluded from delivery. */
    PARTIAL,
    /** The run could not complete, e.g. the provider was unreachable. */
    FAILED
}
