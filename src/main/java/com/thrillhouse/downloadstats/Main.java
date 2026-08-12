package com.thrillhouse.downloadstats;

import com.thrillhouse.downloadstats.model.PackageStats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the download-stats aggregator. Reads its configuration
 * from environment variables, polls the package registry for each tracked
 * package's download counts, aggregates them, and persists the totals.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = requireEnv("REGISTRY_API_BASE_URL");
        String packageNamesRaw = requireEnv("REGISTRY_PACKAGE_NAMES");
        long timeoutMillis = Long.parseLong(System.getenv().getOrDefault("REGISTRY_REQUEST_TIMEOUT", "5000"));
        String dbPath = System.getenv().getOrDefault("STATS_DB_PATH", "stats.db");

        List<String> packageNames = new ArrayList<>();
        for (String name : packageNamesRaw.split(",")) {
            packageNames.add(name.trim());
        }

        RegistryClient registryClient = new RegistryClient(baseUrl, timeoutMillis);
        StatsAggregator aggregator = new StatsAggregator();
        PersistenceRunner persistenceRunner = new PersistenceRunner();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            ensureSchema(connection);
            StatsRepository repository = new StatsRepository(connection);
            StatsQueryService queryService = new StatsQueryService(connection);
            List<PackageStats> accumulated = new ArrayList<>();

            for (String packageName : packageNames) {
                int previousTotal = queryService.lookupPreviousTotal(packageName);
                List<PackageStats> records = registryClient.fetchAllDownloadRecords(packageName);
                aggregator.mergeInto(accumulated, records);
                System.out.println(packageName + ": previous total " + previousTotal);
            }

            int skipped = persistenceRunner.persistAll(accumulated, repository);
            int grandTotal = aggregator.sumDownloads(accumulated);

            if (!aggregator.getFailedPackages().isEmpty()) {
                System.err.println(aggregator.getFailedPackages().size() + " packages failed aggregation: "
                        + aggregator.getFailedPackages());
                System.exit(1);
            }

            System.out.println("Aggregated " + grandTotal + " downloads across " + accumulated.size()
                    + " packages (" + skipped + " already up to date).");
        }
    }

    private static void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS package_stats ("
                    + "package_name TEXT PRIMARY KEY, total_downloads INTEGER NOT NULL)");
        }
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }
}
