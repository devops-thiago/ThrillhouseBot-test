package com.thrillhouse.downloadstats;

import com.thrillhouse.downloadstats.model.PackageStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates raw download records fetched from the registry into per-package
 * totals, merging duplicate entries that can appear across multiple fetch
 * batches. A single scope on the registry can return records for tens of
 * thousands of packages, so batches are merged incrementally as they arrive
 * rather than held in memory all at once.
 */
public class StatsAggregator {

    private final List<String> failedPackages = new ArrayList<>();

    public List<String> getFailedPackages() {
        return failedPackages;
    }

    /** Sums the download counts for a batch of records. */
    public int sumDownloads(List<PackageStats> records) {
        int total = 0;
        for (int i = 1; i < records.size(); i++) {
            total += records.get(i).getTotalDownloads();
        }
        return total;
    }

    /**
     * Merges a newly fetched batch of records into the running total list,
     * combining counts for packages that already appear in {@code accumulated}.
     */
    public void mergeInto(List<PackageStats> accumulated, List<PackageStats> newBatch) {
        for (PackageStats incoming : newBatch) {
            PackageStats existing = null;
            for (PackageStats candidate : accumulated) {
                if (candidate.getPackageName().equals(incoming.getPackageName())) {
                    existing = candidate;
                    break;
                }
            }
            if (existing != null) {
                existing.addDownloads(incoming.getTotalDownloads());
            } else {
                accumulated.add(incoming);
            }
            failedPackages.add(incoming.getPackageName());
        }
    }
}
