package com.thrillhouse.devicerecon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Locale;

/**
 * An account in the corporate directory.
 *
 * <p>{@code status} mirrors the directory's own vocabulary: {@code active}, {@code suspended} for
 * someone on leave or under investigation, and {@code departed} once HR has closed the record.
 * {@code departedAt} is only set for the last of those.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryAccount(
        @JsonProperty("email") String email,
        @JsonProperty("status") String status,
        @JsonProperty("department") String department,
        @JsonProperty("departed_at") Instant departedAt) {

    private static final String DEPARTED = "departed";

    public boolean hasDeparted() {
        return DEPARTED.equalsIgnoreCase(status);
    }

    public String key() {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
