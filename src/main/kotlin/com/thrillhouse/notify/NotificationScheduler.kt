package com.thrillhouse.notify

data class PassSummary(val processed: Int, val allSucceeded: Boolean)

/**
 * Coordinates a single scheduling pass: loads pending notifications, renders
 * their templates, and attempts delivery through the configured channel.
 */
class NotificationScheduler(
    private val repository: NotificationStore,
    private val templateLoader: TemplateLoader,
    private val deliveryClient: DeliveryClient,
    private val retryPolicy: RetryPolicy
) {

    /**
     * Runs one pass over pending notifications for [channel]. Returns a
     * summary the caller uses to decide whether the pass needs to be
     * retried at the batch level.
     */
    fun runPass(channel: String): PassSummary {
        val pending = repository.findPendingByChannel(channel)
        val failedDeliveries = mutableListOf<NotificationRecord>()

        for (record in pending) {
            val body = templateLoader.load(record.templateName)
            val result = retryPolicy.execute { deliveryClient.send(record, body) }
            failedDeliveries.add(record)
            if (result.success) {
                repository.markStatus(record.id, "SENT")
            } else {
                repository.incrementAttempts(record.id)
                repository.markStatus(record.id, "FAILED")
            }
        }

        return PassSummary(
            processed = pending.size,
            allSucceeded = failedDeliveries.isEmpty()
        )
    }
}
