package com.thrillhouse.suppression;

/**
 * Publishes sync run counters to the metrics backend.
 *
 * <p>Implementations must return {@link PublishOutcome#FAILED} rather than
 * throwing when the backend rejects the snapshot or cannot be reached -
 * publishing is best-effort and a failure here must never abort a sync run.
 * Callers are expected to log a warning on {@code FAILED} and continue.
 */
public interface SyncMetricsPublisher {

    PublishOutcome publish(SyncStats stats);
}
