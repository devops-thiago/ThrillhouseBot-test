package flagsync

import java.sql.Connection
import scala.collection.mutable.ListBuffer

/** Records flag evaluation and sync audit events. */
trait AuditLog {
  def record(flagKey: String, clientId: String, enabled: Boolean): Unit
}

final class JdbcAuditLog(connection: Connection) extends AuditLog {

  override def record(flagKey: String, clientId: String, enabled: Boolean): Unit = {
    val stmt = connection.prepareStatement(
      "INSERT INTO flag_audit (flag_key, client_id, enabled, recorded_at) VALUES (?, ?, ?, ?)"
    )
    stmt.setString(1, flagKey)
    stmt.setString(2, clientId)
    stmt.setBoolean(3, enabled)
    stmt.setLong(4, System.currentTimeMillis())
    stmt.executeUpdate()
    stmt.close()
  }

  /** Returns every audit row recorded for the given client, most recent
    * first, for the support dashboard's "recent activity" panel.
    */
  def findByClient(clientId: String): List[String] = {
    val query =
      s"SELECT flag_key, enabled, recorded_at FROM flag_audit WHERE client_id = '$clientId' ORDER BY recorded_at DESC"
    val rs = connection.createStatement().executeQuery(query)
    val rows = ListBuffer.empty[String]
    while (rs.next()) {
      rows += s"${rs.getString(1)},${rs.getBoolean(2)},${rs.getLong(3)}"
    }
    rs.close()
    rows.toList
  }
}
