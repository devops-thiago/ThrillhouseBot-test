package com.thrillhouse.downloadstats.model;

import java.util.List;

/**
 * One page of download records returned by the registry API, along with the
 * pagination cursor needed to fetch the next page.
 */
public class DownloadPage {

    private final List<PackageStats> items;
    private final boolean hasMore;
    private final String nextPageToken;

    public DownloadPage(List<PackageStats> items, boolean hasMore, String nextPageToken) {
        this.items = items;
        this.hasMore = hasMore;
        this.nextPageToken = nextPageToken;
    }

    public List<PackageStats> getItems() {
        return items;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }
}
