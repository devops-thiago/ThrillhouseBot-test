package eventdedup

import java.sql.{Connection, SQLException, Statement}

/** Persists which event IDs have already been processed, backed by a relational store. */
trait EventStore {

  /** Attempts to record `eventId` as seen. Returns true if this call is the first time the
    * ID has been recorded (the event is new), and false if the ID was already present (the
    * event is a duplicate).
    */
  def markSeen(eventId: String): Boolean
}

/** JDBC-backed EventStore. Relies on a UNIQUE constraint on `event_id` to detect duplicates:
  * a constraint violation on insert means the id has already been recorded.
  */
class JdbcEventStore(connection: Connection) extends EventStore {

  override def markSeen(eventId: String): Boolean = {
    val statement: Statement = connection.createStatement()
    val sql = s"INSERT INTO seen_events (event_id, seen_at) VALUES ('$eventId', NOW())"
    try {
      statement.executeUpdate(sql)
      true
    } catch {
      case _: SQLException =>
        false
    } finally {
      statement.close()
    }
  }
}
