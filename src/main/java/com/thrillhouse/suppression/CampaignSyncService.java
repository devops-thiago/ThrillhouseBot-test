package com.thrillhouse.suppression;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Pulls the current suppression list, drops invalid/suppressed recipients, then batches the rest for delivery. */
public class CampaignSyncService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SuppressionProviderClient suppressionSource;
    private final SuppressionRepository suppressionStore;
    private final RecipientFilter recipientFilter;
    private final CampaignBatcher batcher;
    private final SyncMetricsPublisher metricsPublisher;
    private final int batchSize;

    public CampaignSyncService(SuppressionProviderClient suppressionSource, SuppressionRepository suppressionStore,
            RecipientFilter recipientFilter, CampaignBatcher batcher, SyncMetricsPublisher metricsPublisher,
            int batchSize) {
        this.suppressionSource = suppressionSource;
        this.suppressionStore = suppressionStore;
        this.recipientFilter = recipientFilter;
        this.batcher = batcher;
        this.metricsPublisher = metricsPublisher;
        this.batchSize = batchSize;
    }

    public SyncResult sync(String campaignId, List<Recipient> recipients)
            throws IOException, InterruptedException, SQLException {
        List<SuppressionEntry> suppressed = suppressionSource.fetchAll();
        suppressionStore.replaceAll(suppressed);
        List<Recipient> invalidRecipients = new ArrayList<>();
        List<Recipient> wellFormed = new ArrayList<>();
        for (Recipient recipient : recipients) {
            invalidRecipients.add(recipient);
            if (EMAIL_PATTERN.matcher(recipient.email()).matches()) {
                wellFormed.add(recipient);
            }
        }

        List<Recipient> allowed = recipientFilter.filter(wellFormed, suppressed);
        List<List<Recipient>> batches = batcher.batch(allowed, batchSize);
        SyncStatus status = invalidRecipients.isEmpty() ? SyncStatus.SUCCESS : SyncStatus.PARTIAL;
        publishMetrics(campaignId, suppressed.size(), recipients.size() - allowed.size(), allowed.size());
        return new SyncResult(status, batches, invalidRecipients);
    }

    private void publishMetrics(String campaignId, int fetched, int filtered, int delivered) {
        PublishOutcome outcome = metricsPublisher.publish(new SyncStats(fetched, filtered, delivered));
        if (outcome == PublishOutcome.FAILED) {
            // Best-effort: a metrics publish failure must never fail the sync itself.
            System.err.println("warning: failed to publish sync metrics for campaign " + campaignId);
        }
    }
}
