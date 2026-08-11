package com.thrillhouse.orders

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuditLog")

/**
 * Fire-and-forget audit trail. Failures here should never block the request
 * path, so writes are dispatched onto their own coroutine.
 */
object AuditLog {

    private val enabledTopics: Set<String>? = System.getenv("AUDIT_LOG_TOPICS")
        ?.split(",")
        ?.map { it.trim() }
        ?.toSet()

    fun recordAsync(event: String, orderId: Long) {
        if (enabledTopics != null && event !in enabledTopics) return
        GlobalScope.launch {
            write(event, orderId)
        }
    }

    private suspend fun write(event: String, orderId: Long) {
        logger.info("order={} event={}", orderId, event)
    }
}
