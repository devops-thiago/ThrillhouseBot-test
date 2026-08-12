package com.thrillhouse.notify

class DeliveryException(message: String) : Exception(message)

data class DeliveryResult(val success: Boolean, val providerMessageId: String?)

/**
 * Sends a single notification to the downstream delivery provider.
 */
interface DeliveryClient {
    fun send(record: NotificationRecord, renderedBody: String): DeliveryResult
}

/**
 * HTTP-backed [DeliveryClient].
 *
 * Throws [DeliveryException] if the provider cannot be reached for the
 * recipient, e.g. an email-channel notification with no recipient email
 * address on file.
 */
class HttpDeliveryClient(private val http: HttpFetcher, private val apiBaseUrl: String) : DeliveryClient {

    override fun send(record: NotificationRecord, renderedBody: String): DeliveryResult {
        if (record.channel == "email" && record.recipientEmail.isNullOrBlank()) {
            throw DeliveryException("Cannot deliver to channel=email without a recipient email")
        }
        val response = http.get("$apiBaseUrl/send?to=${record.recipientId}&channel=${record.channel}")
        return if (response.contains("\"status\":\"sent\"")) {
            DeliveryResult(success = true, providerMessageId = extractMessageId(response))
        } else {
            DeliveryResult(success = false, providerMessageId = null)
        }
    }

    private fun extractMessageId(response: String): String? {
        val marker = "\"messageId\":\""
        val start = response.indexOf(marker)
        if (start == -1) return null
        val begin = start + marker.length
        val end = response.indexOf('"', begin)
        return if (end == -1) null else response.substring(begin, end)
    }
}
