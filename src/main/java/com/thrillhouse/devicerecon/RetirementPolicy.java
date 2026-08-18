package com.thrillhouse.devicerecon;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Decides what should happen to one enrolled device, and says why. */
public class RetirementPolicy {

    /** What the reconciliation should do with a device. */
    public enum Action {
        /** Leave the enrolment alone. */
        KEEP,
        /** Retire the enrolment in the MDM. */
        RETIRE,
        /** Neither is safe without a human: surface it in the report. */
        REVIEW
    }

    /** An action and the sentence that goes into the report and the MDM audit log. */
    public record Decision(Action action, String reason) {}

    private final Set<String> exemptPlatforms;
    private final int graceDays;

    public RetirementPolicy(Collection<String> exemptPlatforms, int graceDays) {
        this.exemptPlatforms = exemptPlatforms.stream()
                .map(platform -> platform.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.graceDays = graceDays;
    }

    /**
     * Evaluates one device against its owner's directory account. {@code account} is null when the
     * directory has no record of the owner at all.
     *
     * <p>The order of the checks is the policy: an unknown owner is never retired, a device on an
     * exempt platform is never retired even when its enrolling owner has left, and a departure the
     * directory cannot date is escalated rather than guessed at.
     */
    public Decision evaluate(EnrolledDevice device, DirectoryAccount account, Instant now) {
        if (device.retired()) {
            return new Decision(Action.KEEP, "enrolment is already retired");
        }
        if (account == null) {
            return new Decision(Action.REVIEW, "owner " + device.ownerKey() + " is not in the directory");
        }
        if (!account.hasDeparted()) {
            return new Decision(Action.KEEP, "owner is " + account.status());
        }
        if (exemptPlatforms.contains(device.platform().toLowerCase(Locale.ROOT))) {
            // Shared and kiosk devices are enrolled under whoever set them up, so their owner
            // leaving says nothing about whether the device is still in use.
            return new Decision(Action.REVIEW,
                    "owner has departed but platform " + device.platform() + " is exempt from retirement");
        }
        if (account.departedAt() == null) {
            return new Decision(Action.REVIEW, "owner has departed but the directory carries no departure date");
        }

        Instant retireFrom = account.departedAt().plus(graceDays, ChronoUnit.DAYS);
        if (now.isBefore(retireFrom)) {
            return new Decision(Action.KEEP,
                    "owner departed on " + account.departedAt() + ", grace window ends " + retireFrom);
        }
        return new Decision(Action.RETIRE, "owner departed on " + account.departedAt()
                + ", past the " + graceDays + " day grace window");
    }
}
