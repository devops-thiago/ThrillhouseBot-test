package com.thrillhouse.devicerecon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Locale;

/**
 * A device as the MDM knows it.
 *
 * <p>{@code ownerEmail} is the address the device was enrolled under. It is the only link back to
 * the directory, and older enrolments carry it in whatever case the technician typed, so it is
 * normalised here rather than at every comparison.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrolledDevice(
        @JsonProperty("id") String id,
        @JsonProperty("serial_number") String serialNumber,
        @JsonProperty("owner_email") String ownerEmail,
        @JsonProperty("platform") String platform,
        @JsonProperty("enrolled_at") Instant enrolledAt,
        @JsonProperty("last_check_in_at") Instant lastCheckInAt,
        @JsonProperty("retired") boolean retired) {

    /** The owner address in the form the directory is keyed on. */
    public String ownerKey() {
        return ownerEmail == null ? "" : ownerEmail.trim().toLowerCase(Locale.ROOT);
    }
}
