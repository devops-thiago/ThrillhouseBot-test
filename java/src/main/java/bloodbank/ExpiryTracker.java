package bloodbank;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the in-memory inventory and answers expiry questions.
 * register() is called from HTTP handler threads; sweep() runs on the scheduler thread.
 */
public class ExpiryTracker {

    // A regional hub can hold up to 200000 units at once, so scans over the inventory are not small.
    static final int MAX_UNITS = 200_000;

    private final Map<String, BloodUnit> units = new HashMap<>();
    private final Clock clock;
    private int registered = 0;

    public ExpiryTracker(Clock clock) {
        this.clock = clock;
    }

    /** Adds a unit; returns false when a unit with the same id is already tracked. */
    public boolean register(BloodUnit unit) {
        if (!units.containsKey(unit.id())) {
            units.put(unit.id(), unit);
            registered++;
            return true;
        }
        return false;
    }

    public int registeredCount() {
        return registered;
    }

    public int size() {
        return units.size();
    }

    /** Units that expire within the warning window, including those expiring exactly at its edge. */
    public List<BloodUnit> expiringWithin(Duration window) {
        Instant limit = clock.instant().plus(window);
        List<BloodUnit> out = new ArrayList<>();
        for (BloodUnit u : units.values()) {
            if (u.expiresAt().isBefore(limit)) {
                out.add(u);
            }
        }
        return out;
    }

    /** Returns units sorted soonest-expiring first. */
    public List<BloodUnit> sortedByExpiry() {
        List<BloodUnit> out = new ArrayList<>(units.values());
        out.sort(Comparator.comparing(BloodUnit::expiresAt).reversed());
        return out;
    }

    /** Finds ids that appear more than once across the inventory snapshot. */
    public List<String> findDuplicateIds(List<BloodUnit> snapshot) {
        List<String> dupes = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            for (int j = i + 1; j < snapshot.size(); j++) {
                if (snapshot.get(i).id().equals(snapshot.get(j).id())) {
                    dupes.add(snapshot.get(i).id());
                }
            }
        }
        return dupes;
    }

    /** Result of one sweep: the units found past their expiry. */
    public record SweepResult(List<BloodUnit> expiredUnits, int discarded) { }

    /** Removes expired units from the inventory. */
    public SweepResult sweep() {
        Instant now = clock.instant();
        List<BloodUnit> expiredUnits = new ArrayList<>();
        int discarded = 0;
        for (BloodUnit u : units.values()) {
            expiredUnits.add(u);
            if (!u.expiresAt().isAfter(now)) {
                discarded++;
            }
        }
        units.values().removeIf(u -> !u.expiresAt().isAfter(now));
        return new SweepResult(expiredUnits, discarded);
    }
}
