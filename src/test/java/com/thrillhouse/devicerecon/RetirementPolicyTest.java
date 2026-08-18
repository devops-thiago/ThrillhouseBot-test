package com.thrillhouse.devicerecon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thrillhouse.devicerecon.RetirementPolicy.Action;
import com.thrillhouse.devicerecon.RetirementPolicy.Decision;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetirementPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");
    private static final int GRACE_DAYS = 30;

    private final RetirementPolicy policy = new RetirementPolicy(List.of("kiosk", "shared-ipad"), GRACE_DAYS);

    private static EnrolledDevice device(String platform, boolean retired) {
        return new EnrolledDevice("d-1", "SN-1", "Sam.Vimes@example.com", platform,
                NOW.minus(400, ChronoUnit.DAYS), NOW.minus(2, ChronoUnit.DAYS), retired);
    }

    private static DirectoryAccount departed(int daysAgo) {
        return new DirectoryAccount("sam.vimes@example.com", "departed", "engineering",
                NOW.minus(daysAgo, ChronoUnit.DAYS));
    }

    @Test
    void retiresADeviceWhoseOwnerLeftBeforeTheGraceWindowClosed() {
        Decision decision = policy.evaluate(device("macos", false), departed(GRACE_DAYS + 1), NOW);

        assertEquals(Action.RETIRE, decision.action());
        assertTrue(decision.reason().contains("grace window"), decision.reason());
    }

    @Test
    void keepsADeviceWhoseOwnerIsStillInsideTheGraceWindow() {
        Decision decision = policy.evaluate(device("macos", false), departed(GRACE_DAYS - 1), NOW);

        assertEquals(Action.KEEP, decision.action());
    }

    @Test
    void keepsADeviceWhoseOwnerIsActiveOrSuspended() {
        DirectoryAccount active = new DirectoryAccount("sam.vimes@example.com", "active", "engineering", null);
        DirectoryAccount suspended = new DirectoryAccount("sam.vimes@example.com", "suspended", "engineering", null);

        assertEquals(Action.KEEP, policy.evaluate(device("macos", false), active, NOW).action());
        assertEquals(Action.KEEP, policy.evaluate(device("macos", false), suspended, NOW).action());
    }

    @Test
    void neverRetiresADeviceWhoseOwnerTheDirectoryDoesNotKnow() {
        Decision decision = policy.evaluate(device("macos", false), null, NOW);

        assertEquals(Action.REVIEW, decision.action());
        assertTrue(decision.reason().contains("sam.vimes@example.com"), decision.reason());
    }

    @Test
    void escalatesAnExemptPlatformInsteadOfRetiringIt() {
        Decision decision = policy.evaluate(device("KIOSK", false), departed(GRACE_DAYS + 90), NOW);

        assertEquals(Action.REVIEW, decision.action());
    }

    @Test
    void escalatesADepartureTheDirectoryCannotDate() {
        DirectoryAccount undated = new DirectoryAccount("sam.vimes@example.com", "departed", "engineering", null);

        assertEquals(Action.REVIEW, policy.evaluate(device("macos", false), undated, NOW).action());
    }

    @Test
    void leavesAnAlreadyRetiredEnrolmentAlone() {
        assertEquals(Action.KEEP, policy.evaluate(device("macos", true), departed(GRACE_DAYS + 90), NOW).action());
    }
}
