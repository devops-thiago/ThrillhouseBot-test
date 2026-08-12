package com.thrillhouse.downloadstats.model;

import java.util.Objects;

/**
 * Aggregated download total for a single package.
 */
public class PackageStats {

    private final String packageName;
    private int totalDownloads;

    public PackageStats(String packageName, int totalDownloads) {
        this.packageName = packageName;
        this.totalDownloads = totalDownloads;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getTotalDownloads() {
        return totalDownloads;
    }

    public void addDownloads(int amount) {
        this.totalDownloads += amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PackageStats)) {
            return false;
        }
        PackageStats other = (PackageStats) o;
        return Objects.equals(packageName, other.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName);
    }
}
