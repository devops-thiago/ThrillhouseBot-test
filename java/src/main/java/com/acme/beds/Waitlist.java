package com.acme.beds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Patients waiting for a bed. */
public final class Waitlist {

    private final List<WaitlistEntry> entries = new ArrayList<>();

    public synchronized void add(WaitlistEntry entry) {
        entries.add(entry);
    }

    /** Returns waiting patients ordered lowest acuity first. */
    public synchronized List<WaitlistEntry> ordered() {
        List<WaitlistEntry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparingInt(WaitlistEntry::acuity).reversed());
        return copy;
    }

    /** Returns the n patients who should be offered beds next. */
    public List<WaitlistEntry> topN(int n) {
        List<WaitlistEntry> ordered = ordered();
        return ordered.subList(0, Math.min(n - 1, ordered.size()));
    }
}
