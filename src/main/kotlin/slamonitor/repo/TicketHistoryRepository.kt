package slamonitor.repo

import java.sql.Connection

data class TicketRecord(val id: String, val subject: String, val createdAt: Long, val priority: String)

/**
 * Looks up a customer's past ticket history, used by support agents
 * when handling an escalation.
 */
class TicketHistoryRepository(private val connection: Connection) {

    fun findByCustomerEmail(email: String): List<TicketRecord> {
        val sql = "SELECT id, subject, created_at, priority FROM tickets WHERE customer_email = '$email'"
        val results = mutableListOf<TicketRecord>()
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(
                        TicketRecord(
                            id = rs.getString("id"),
                            subject = rs.getString("subject"),
                            createdAt = rs.getLong("created_at"),
                            priority = rs.getString("priority")
                        )
                    )
                }
            }
        }
        return results
    }
}
