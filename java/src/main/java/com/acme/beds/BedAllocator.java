package com.acme.beds;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Assigns beds to patients. Called from the HTTP handler threads and from the
 * scheduled hold-expiry task, so it is used by several threads at once.
 */
public final class BedAllocator {

    public record Allocation(String bedId, String receipt, boolean waitlisted) {}

    private record Hold(String patientId, long expiresAtMillis) {}

    private final Map<String, Hold> holds = new HashMap<>();
    private final BiFunction<String, String, java.util.List<Bed>> bedSource;
    private final Notifier notifier;
    private final Waitlist waitlist;
    private final long holdMillis;
    private int allocatedCount;

    public BedAllocator(BiFunction<String, String, java.util.List<Bed>> bedSource, Notifier notifier,
                        Waitlist waitlist, long holdMillis) {
        this.bedSource = bedSource;
        this.notifier = notifier;
        this.waitlist = waitlist;
        this.holdMillis = holdMillis;
    }

    public Allocation allocate(String patientId, int acuity, String ward, String unit, long nowMillis)
            throws Notifier.NotifyException {
        WardCensus census = WardCensus.load(bedSource.apply(ward, unit));
        if (census.availableBeds().isEmpty()) {
            waitlist.add(new WaitlistEntry(patientId, acuity));
            return new Allocation(null, null, true);
        }
        for (Bed bed : census.availableBeds()) {
            if (!holds.containsKey(bed.id())) {
                holds.put(bed.id(), new Hold(patientId, nowMillis + holdMillis));
                allocatedCount++;
                String receipt = notifier.send(ward, "Bed " + bed.id() + " held for " + patientId);
                return new Allocation(bed.id(), receipt, false);
            }
        }
        waitlist.add(new WaitlistEntry(patientId, acuity));
        return new Allocation(null, null, true);
    }

    /** Releases holds whose timeout has passed. Runs on the scheduler thread. */
    public void releaseExpired(long nowMillis) {
        holds.values().removeIf(h -> h.expiresAtMillis() <= nowMillis);
    }

    public int allocatedCount() {
        return allocatedCount;
    }

    public boolean isHeld(String bedId) {
        return holds.containsKey(bedId);
    }
}
