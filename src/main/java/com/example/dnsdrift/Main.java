package com.example.dnsdrift;

import com.example.dnsdrift.client.RegistrarClient;
import com.example.dnsdrift.config.DriftConfig;
import com.example.dnsdrift.model.DriftEntry;
import com.example.dnsdrift.model.TrackedDomain;
import com.example.dnsdrift.repository.DriftRepository;
import com.example.dnsdrift.service.DriftDetectorService;
import java.net.http.HttpClient;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

/** Entry point for the drift detector: polls the registrar and reports mismatches. */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        DriftConfig config = DriftConfig.fromEnvironment();
        RegistrarClient client = new RegistrarClient(
                HttpClient.newHttpClient(), config.getRegistrarBaseUrl(), config.getRegistrarApiKey());
        DriftDetectorService detector = new DriftDetectorService();

        try (Connection connection = DriverManager.getConnection(config.getDatabaseUrl())) {
            DriftRepository repository = new DriftRepository(connection);

            if (args.length >= 2 && "query".equals(args[0])) {
                for (DriftEntry entry : repository.findByDomain(args[1])) {
                    System.out.println(entry.domain() + " " + entry.recordType() + " "
                            + entry.expectedValue() + " -> " + entry.actualValue());
                }
                return;
            }

            for (String domainName : config.getTrackedDomainNames()) {
                TrackedDomain tracked = new TrackedDomain(domainName, expectedRecordsFor(domainName));
                List<DriftEntry> driftEntries = detector.refreshAndDetect(client, tracked);

                if (!detector.getInvalidRecords().isEmpty()) {
                    System.out.println("Skipping persistence for " + domainName + ": invalid records present");
                    continue;
                }

                detector.persistDrift(repository, driftEntries);
                for (DriftEntry entry : driftEntries) {
                    System.out.println("Drift detected: " + entry.domain() + " " + entry.recordType());
                }
            }
        }
    }

    // In a real deployment this would come from a config store; hardcoded here for brevity.
    private static Map<String, String> expectedRecordsFor(String domainName) {
        return Map.of("A", "203.0.113.10", "MX", "mail." + domainName);
    }
}
