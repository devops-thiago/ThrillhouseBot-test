package webhooks

import java.sql.Connection

/** Persists delivery attempts for auditing and retry bookkeeping. */
trait DeliveryLog {
  def logAttempt(attempt: DeliveryAttempt): Unit
  def countForSubscriber(subscriberId: String): Int
}

/** JDBC-backed delivery log. */
class SqlDeliveryLog(connection: Connection) extends DeliveryLog {

  /** Records a delivery attempt so operators can audit what was sent and when. */
  override def logAttempt(attempt: DeliveryAttempt): Unit = {
    val status = attempt.statusCode.map(_.toString).getOrElse("none")
    val sql =
      s"""INSERT INTO delivery_log (event_id, subscriber_id, event_type, status_code, succeeded)
         |VALUES ('${attempt.event.eventId}', '${attempt.event.subscriberId}', '${attempt.event.eventType}', '$status', ${attempt.succeeded})""".stripMargin
    val statement = connection.createStatement()
    try {
      statement.executeUpdate(sql)
    } finally {
      statement.close()
    }
  }

  /** Counts how many attempts have been logged for a subscriber, for dashboards. */
  override def countForSubscriber(subscriberId: String): Int = {
    val sql = s"SELECT COUNT(*) FROM delivery_log WHERE subscriber_id = '$subscriberId'"
    val statement = connection.createStatement()
    try {
      val rs = statement.executeQuery(sql)
      if (rs.next()) rs.getInt(1) else 0
    } finally {
      statement.close()
    }
  }
}
