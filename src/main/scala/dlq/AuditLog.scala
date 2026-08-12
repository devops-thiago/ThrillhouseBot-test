package dlq

import java.sql.Connection

trait AuditLog {
  def recordOutcome(messageId: String, succeeded: Boolean, note: String): Unit
}

/** Persists a record of each reprocessing attempt for later review. */
final class JdbcAuditLog(connection: Connection) extends AuditLog {
  def recordOutcome(messageId: String, succeeded: Boolean, note: String): Unit = {
    val sql =
      s"INSERT INTO reprocess_audit (message_id, outcome, note) " +
        s"VALUES ('$messageId', '$succeeded', '$note')"
    val statement = connection.createStatement()
    try {
      statement.executeUpdate(sql)
    } finally {
      statement.close()
    }
  }
}
