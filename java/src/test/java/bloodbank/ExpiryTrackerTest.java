package bloodbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpiryTrackerTest {

    private static final Instant NOW = Instant.parse("2025-03-10T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private BloodUnit unit(String id, long daysLeft) {
        return new BloodUnit(id, "central", "O+", "red-cells", NOW.minus(Duration.ofDays(30)),
                NOW.plus(Duration.ofDays(daysLeft)));
    }

    @Test
    void registerRejectsDuplicateIds() {
        ExpiryTracker tracker = new ExpiryTracker(clock);
        assertTrue(tracker.register(unit("u1", 5)));
        assertFalse(tracker.register(unit("u1", 5)));
        assertEquals(1, tracker.size());
    }

    @Test
    void unitExpiringExactlyAtWindowEdgeIsFlagged() {
        ExpiryTracker tracker = new ExpiryTracker(clock);
        tracker.register(unit("edge", 3));
        tracker.register(unit("far", 10));
        List<BloodUnit> soon = tracker.expiringWithin(Duration.ofDays(3));
        assertEquals(1, soon.size());
        assertEquals("edge", soon.get(0).id());
    }

    @Test
    void findDuplicateIdsReportsRepeats() {
        ExpiryTracker tracker = new ExpiryTracker(clock);
        List<BloodUnit> snapshot = new ArrayList<>(List.of(unit("a", 1), unit("b", 1), unit("a", 2)));
        assertEquals(List.of("a"), tracker.findDuplicateIds(snapshot));
    }

    @Test
    void sweepRemovesExpiredUnits() {
        ExpiryTracker tracker = new ExpiryTracker(clock);
        tracker.register(unit("old", -1));
        tracker.register(unit("fresh", 4));
        ExpiryTracker.SweepResult result = tracker.sweep();
        assertEquals(1, result.discarded());
        assertEquals(1, tracker.size());
    }

    @Test
    void alertIsSentWhenSweepDiscardsUnits() {
        ExpiryTracker tracker = new ExpiryTracker(clock);
        tracker.register(unit("old", -1));
        List<String> sent = new ArrayList<>();
        AlertService alerts = new AlertService(sent::add);
        assertTrue(alerts.notifyExpired(tracker.sweep()));
        assertEquals(1, sent.size());
    }

    @Test
    void offlineSensorIsHandled() {
        // Stub reports "offline" with a -999 sentinel instead of throwing.
        FridgeSensor stub = () -> -999.0;
        FridgeMonitor.Status status = new FridgeMonitor(stub).check();
        assertNotEquals(FridgeMonitor.Status.OK, status);
    }

    @Test
    void healthyFridgeIsOk() {
        assertEquals(FridgeMonitor.Status.OK, new FridgeMonitor(() -> 4.0).check());
    }
}
