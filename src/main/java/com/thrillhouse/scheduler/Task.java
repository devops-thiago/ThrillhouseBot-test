package com.thrillhouse.scheduler;

import java.time.Instant;

/**
 * A scheduled unit of work.
 *
 * <p>{@code intervalSeconds} is the fixed cadence between runs; recurring tasks always set
 * it, while ad-hoc one-off tasks leave it {@code null} and rely solely on {@code nextRunAt}.
 */
public record Task(String id, String name, Integer intervalSeconds, Instant nextRunAt, String payload) {
}
