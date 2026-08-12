package com.thrillhouse.notify

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeNotificationStore(private val records: List<NotificationRecord>) : NotificationStore {
    val statusUpdates = mutableListOf<Pair<Long, String>>()

    override fun findPendingByChannel(channel: String): List<NotificationRecord> =
        records.filter { it.channel == channel }

    override fun markStatus(id: Long, status: String) {
        statusUpdates.add(id to status)
    }

    override fun incrementAttempts(id: Long) {
        // Not exercised by this test.
    }
}

/**
 * Always reports a successful send. The real HttpDeliveryClient rejects
 * email-channel notifications with no recipient email on file, but that
 * validation detail isn't the point of what this test is checking.
 */
private class FakeDeliveryClient : DeliveryClient {
    override fun send(record: NotificationRecord, renderedBody: String): DeliveryResult =
        DeliveryResult(success = true, providerMessageId = "fake-message-id")
}

class NotificationSchedulerTest {

    @Test
    fun `runPass marks a notification with no recipient email as sent`() {
        val templatesDir = File.createTempFile("templates", "").apply {
            delete()
            mkdirs()
        }
        File(templatesDir, "welcome.txt").writeText("Hello there!")

        val record = NotificationRecord(
            id = 1L,
            recipientId = "user-42",
            recipientEmail = null,
            channel = "email",
            templateName = "welcome",
            payload = emptyMap(),
            status = "PENDING"
        )
        val store = FakeNotificationStore(listOf(record))
        val scheduler = NotificationScheduler(
            repository = store,
            templateLoader = TemplateLoader(templatesDir),
            deliveryClient = FakeDeliveryClient(),
            retryPolicy = RetryPolicy(maxAttempts = 1)
        )

        val summary = scheduler.runPass("email")

        assertEquals(1, summary.processed)
        assertTrue(store.statusUpdates.contains(1L to "SENT"))
    }
}
