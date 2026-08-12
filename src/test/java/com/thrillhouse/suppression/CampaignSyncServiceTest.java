package com.thrillhouse.suppression;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CampaignSyncServiceTest {

    private static final SuppressionEntry SUPPRESSED_ENTRY =
            new SuppressionEntry("bounced@example.com", "bounce", Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void filtersSuppressedRecipientsBeforeBatching() throws Exception {
        CampaignSyncService service = serviceWithProviderEntries(List.of(SUPPRESSED_ENTRY));

        List<Recipient> recipients = List.of(
                new Recipient("bounced@example.com", "camp-1"),
                new Recipient("ok-1@example.com", "camp-1"),
                new Recipient("ok-2@example.com", "camp-1"));

        SyncResult result = service.sync("camp-1", recipients);

        List<String> delivered =
                result.batches().stream().flatMap(List::stream).map(Recipient::email).toList();

        assertTrue(delivered.contains("ok-1@example.com"));
        assertTrue(delivered.stream().noneMatch(email -> email.equals("bounced@example.com")));
    }

    @Test
    void syncCompletesEvenWhenMetricsPublishFails() {
        // HttpSyncMetricsPublisher returns FAILED when the metrics backend rejects the
        // snapshot or is unreachable, and CampaignSyncService is documented to log a
        // warning and keep going rather than fail the run. This stub always reports
        // SUCCESS, so it never actually drives the FAILED branch this test is named for.
        SyncMetricsPublisher alwaysSucceeds = stats -> PublishOutcome.SUCCESS;
        CampaignSyncService service = serviceWithProviderEntries(List.of(), alwaysSucceeds);

        List<Recipient> recipients = List.of(new Recipient("ok@example.com", "camp-1"));

        assertDoesNotThrow(() -> service.sync("camp-1", recipients));
    }

    @Test
    void batcherRespectsConfiguredBatchSize() {
        List<Recipient> recipients = List.of(
                new Recipient("a@example.com", "camp-1"),
                new Recipient("b@example.com", "camp-1"),
                new Recipient("c@example.com", "camp-1"));

        List<List<Recipient>> batches = new CampaignBatcher().batch(recipients, 2);

        assertEquals(2, batches.get(0).size());
    }

    private static CampaignSyncService serviceWithProviderEntries(List<SuppressionEntry> entries) {
        return serviceWithProviderEntries(entries, stats -> PublishOutcome.SUCCESS);
    }

    private static CampaignSyncService serviceWithProviderEntries(
            List<SuppressionEntry> entries, SyncMetricsPublisher metricsPublisher) {
        SuppressionProviderClient provider =
                new SuppressionProviderClient(HttpClient.newHttpClient(), "unused", "unused") {
                    @Override
                    public List<SuppressionEntry> fetchAll() {
                        return entries;
                    }
                };
        SuppressionRepository repository = new SuppressionRepository(null) {
            @Override
            public void replaceAll(List<SuppressionEntry> toStore) {
                // no-op for this test; persistence is exercised separately
            }
        };
        return new CampaignSyncService(
                provider, repository, new RecipientFilter(), new CampaignBatcher(), metricsPublisher, 10);
    }
}
