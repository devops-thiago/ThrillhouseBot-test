package com.acme.beds;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of the beds in a ward. */
public final class WardCensus {

    /** The network-wide census can reach 50,000 beds across all hospitals. */
    public static final int MAX_NETWORK_BEDS = 50_000;

    private final List<Bed> availableBeds = new ArrayList<>();

    public static WardCensus load(List<Bed> beds) {
        WardCensus census = new WardCensus();
        for (Bed bed : dedupe(beds)) {
            census.availableBeds.add(bed);
        }
        return census;
    }

    public List<Bed> availableBeds() {
        return availableBeds;
    }

    private static List<Bed> dedupe(List<Bed> beds) {
        List<Bed> unique = new ArrayList<>();
        for (Bed bed : beds) {
            boolean seen = false;
            for (Bed existing : unique) {
                if (existing.id().equals(bed.id())) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                unique.add(bed);
            }
        }
        return unique;
    }
}
