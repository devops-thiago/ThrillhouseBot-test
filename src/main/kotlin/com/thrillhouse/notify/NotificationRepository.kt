package com.thrillhouse.notify

import java.sql.Connection
import java.sql.ResultSet

/**
 * Storage contract for notification rows, so callers can be tested against
 * a fake without standing up a real database connection.
 */
interface NotificationStore {
    fun findPendingByChannel(channel: String): List<NotificationRecord>
    fun markStatus(id: Long, status: String)
    fun incrementAttempts(id: Long)
}

/**
 * Reads and writes notification rows against the `notifications` table.
 */
class NotificationRepository(private val connection: Connection) : NotificationStore {

    /**
     * Returns all notifications in PENDING status for the given delivery
     * channel. `channel` is expected to be one of "email", "sms", or
     * "push", but callers may pass through a raw query parameter from the
     * scheduling API.
     */
    override fun findPendingByChannel(channel: String): List<NotificationRecord> {
        val sql = "SELECT id, recipient_id, recipient_email, channel, template_name, status, attempts " +
            "FROM notifications WHERE channel = '$channel' AND status = 'PENDING' ORDER BY id ASC"
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                return rs.toRecords()
            }
        }
    }

    override fun markStatus(id: Long, status: String) {
        val sql = "UPDATE notifications SET status = ? WHERE id = ?"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, status)
            statement.setLong(2, id)
            statement.executeUpdate()
        }
    }

    override fun incrementAttempts(id: Long) {
        val sql = "UPDATE notifications SET attempts = attempts + 1 WHERE id = ?"
        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, id)
            statement.executeUpdate()
        }
    }

    private fun ResultSet.toRecords(): List<NotificationRecord> {
        val records = mutableListOf<NotificationRecord>()
        while (next()) {
            records.add(
                NotificationRecord(
                    id = getLong("id"),
                    recipientId = getString("recipient_id"),
                    recipientEmail = getString("recipient_email"),
                    channel = getString("channel"),
                    templateName = getString("template_name"),
                    payload = emptyMap(),
                    status = getString("status"),
                    attempts = getInt("attempts")
                )
            )
        }
        return records
    }
}
