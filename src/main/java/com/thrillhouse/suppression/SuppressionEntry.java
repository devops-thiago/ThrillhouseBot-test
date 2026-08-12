package com.thrillhouse.suppression;

import java.time.Instant;

/** A suppressed address on record with the provider (bounce, complaint, manual). */
public record SuppressionEntry(String email, String reason, Instant addedAt) {
}
