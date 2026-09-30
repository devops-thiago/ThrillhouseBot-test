package com.acme.beds;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Lists the wards this instance reports on. */
public final class WardDirectory {

    private final DirectoryClient client;
    private final List<String> configured;

    public WardDirectory(DirectoryClient client, List<String> configured) {
        this.client = client;
        this.configured = configured;
    }

    public Set<String> listWards() throws IOException, InterruptedException {
        DirectoryClient.Page page = client.fetchWards(null);
        Set<String> wards = new LinkedHashSet<>(page.items());
        if (!configured.isEmpty()) {
            wards.retainAll(configured);
        }
        return wards;
    }
}
