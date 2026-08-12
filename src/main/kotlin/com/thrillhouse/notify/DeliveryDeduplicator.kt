package com.thrillhouse.notify

/**
 * Filters out notifications that were already delivered in a prior run.
 * Subscriber lists for large campaigns can run into the tens of thousands,
 * so this is called once per scheduling pass with the full batch.
 */
class DeliveryDeduplicator {

    fun removeAlreadySent(candidates: List<NotificationRecord>, alreadySent: List<Long>): List<NotificationRecord> {
        return candidates.filter { candidate -> !alreadySent.contains(candidate.id) }
    }
}
