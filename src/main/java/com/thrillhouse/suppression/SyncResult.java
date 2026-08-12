package com.thrillhouse.suppression;

import java.util.List;

/** Outcome of one {@link CampaignSyncService#sync(String, List)} call. */
public record SyncResult(SyncStatus status, List<List<Recipient>> batches, List<Recipient> invalidRecipients) {
}
