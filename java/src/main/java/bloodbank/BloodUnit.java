package bloodbank;

import java.time.Instant;

/** One donated unit of a blood product held at a collection site. */
public record BloodUnit(String id, String site, String bloodType, String product,
                        Instant collectedAt, Instant expiresAt) {

    public BloodUnit {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
    }
}
