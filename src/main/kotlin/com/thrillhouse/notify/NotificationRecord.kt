package com.thrillhouse.notify

/**
 * A single notification queued for delivery to a recipient through one
 * channel (email, sms, or push).
 */
data class NotificationRecord(
    val id: Long,
    val recipientId: String,
    val recipientEmail: String?,
    val channel: String,
    val templateName: String,
    val payload: Map<String, String>,
    val status: String,
    val attempts: Int = 0
)
