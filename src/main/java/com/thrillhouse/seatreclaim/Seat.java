package com.thrillhouse.seatreclaim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * One licensed seat as reported by the identity provider. {@code last_active_at} is serialised as
 * null for seats that were provisioned but never signed in; the provider still lists those seats.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Seat(
        @JsonProperty("id") String id,
        @JsonProperty("email") String email,
        @JsonProperty("department") String department,
        @JsonProperty("role") String role,
        @JsonProperty("last_active_at") Instant lastActiveAt) {}
