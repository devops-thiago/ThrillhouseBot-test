package com.thrillhouse.suppression;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Entry point: {@code sync <campaignId> <jdbcUrl>} or {@code search <domain> <jdbcUrl>}. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: (sync <campaignId>|search <domain>) <jdbcUrl>");
            System.exit(1);
        }
        String baseUrl = requireEnv("SUPPRESSION_PROVIDER_BASE_URL");
        String apiKey = requireEnv("SUPPRESSION_PROVIDER_API_KEY");
        int batchSize = Integer.parseInt(System.getenv().getOrDefault("CAMPAIGN_BATCH_SIZE", "20"));
        long timeoutMillis = Long.parseLong(System.getenv().getOrDefault("SUPPRESSION_SYNC_TIMEOUT", "5000"));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMillis)).build();
        try (Connection connection = DriverManager.getConnection(args[2])) {
            SuppressionRepository repository = new SuppressionRepository(connection);

            if ("search".equals(args[0])) {
                for (SuppressionEntry entry : repository.findByDomain(args[1])) {
                    System.out.println(entry.email() + " - " + entry.reason());
                }
                return;
            }

            CampaignSyncService service = new CampaignSyncService(
                    new SuppressionProviderClient(httpClient, baseUrl, apiKey),
                    repository,
                    new RecipientFilter(),
                    new CampaignBatcher(),
                    new HttpSyncMetricsPublisher(httpClient, baseUrl + "/v1/metrics"),
                    batchSize);

            SyncResult result = service.sync(args[1], readRecipients(args[1]));
            System.out.println("status=" + result.status()
                    + " batches=" + result.batches().size()
                    + " invalid=" + result.invalidRecipients().size());
        } catch (SQLException e) {
            System.err.println("database error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing required environment variable " + name);
        }
        return value;
    }

    private static List<Recipient> readRecipients(String campaignId) throws Exception {
        List<Recipient> recipients = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String email = line.strip();
                if (!email.isEmpty()) {
                    recipients.add(new Recipient(email, campaignId));
                }
            }
        }
        return recipients;
    }
}
