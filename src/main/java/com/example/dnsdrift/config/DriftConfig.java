package com.example.dnsdrift.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Loads runtime configuration for the drift detector from environment variables. */
public final class DriftConfig {

    private final String registrarBaseUrl;
    private final String registrarApiKey;
    private final String trackedDomains;
    private final String databaseUrl;
    private final int pollIntervalSeconds;

    private DriftConfig(
            String registrarBaseUrl,
            String registrarApiKey,
            String trackedDomains,
            String databaseUrl,
            int pollIntervalSeconds) {
        this.registrarBaseUrl = registrarBaseUrl;
        this.registrarApiKey = registrarApiKey;
        this.trackedDomains = trackedDomains;
        this.databaseUrl = databaseUrl;
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public static DriftConfig fromEnvironment() {
        String baseUrl = System.getenv("REGISTRAR_BASE_URL");
        String apiKey = System.getenv("REGISTRAR_API_KEY");
        String domains = System.getenv("TRACKED_DOMAINS");
        String dbUrl = System.getenv("DATABASE_URL");
        String pollInterval = System.getenv("POLL_INTERVAL_SECONDS");
        int interval = pollInterval != null ? Integer.parseInt(pollInterval) : 300;
        return new DriftConfig(baseUrl, apiKey, domains, dbUrl, interval);
    }

    public String getRegistrarBaseUrl() {
        return registrarBaseUrl;
    }

    public String getRegistrarApiKey() {
        return registrarApiKey;
    }

    /** Parses {@code TRACKED_DOMAINS} as a comma-separated list, trimming whitespace around each entry. */
    public List<String> getTrackedDomainNames() {
        if (trackedDomains == null || trackedDomains.isBlank()) {
            return List.of();
        }
        return Arrays.stream(trackedDomains.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }
}
