package dlq

import java.sql.DriverManager

/** Entry point: polls the dead-letter queue once and reprocesses everything found. */
object Main {

  def main(args: Array[String]): Unit = {
    val queueUrl = sys.env.getOrElse(
      "DLQ_QUEUE_URL",
      throw new IllegalStateException("DLQ_QUEUE_URL is required")
    )
    val primaryQueueUrl = sys.env.getOrElse(
      "DLQ_PRIMARY_QUEUE_URL",
      throw new IllegalStateException("DLQ_PRIMARY_QUEUE_URL is required")
    )
    val maxRetries = sys.env.get("DLQ_MAX_RETRIES").map(_.toInt).getOrElse(3)
    val alertEmails = sys.env
      .getOrElse("DLQ_ALERT_EMAILS", "")
      .split(",")
      .map(_.trim)
      .filter(_.nonEmpty)
    val dbUrl = sys.env.getOrElse("DLQ_AUDIT_DB_URL", "jdbc:postgresql://localhost:5432/dlq")

    val http = new JdkHttpGateway()
    val reader = new HttpDlqReader(queueUrl, http)
    val ackClient = new HttpAckClient(queueUrl, http)
    val processor = new RepublishProcessor(http, primaryQueueUrl)
    val connection = DriverManager.getConnection(dbUrl)
    val auditLog = new JdbcAuditLog(connection)
    val reprocessor = new Reprocessor(ackClient, auditLog, processor, maxRetries)

    try {
      val batch = reader.fetchAllMessages()
      val result = reprocessor.reprocessBatch(batch)

      // A batch only needs an alert when some messages truly failed; a
      // fully healthy run stays silent so on-call is not paged for nothing.
      if (result.succeeded.size == batch.size) {
        println(s"reprocessed ${batch.size} messages, batch healthy")
      } else {
        val failedCount = batch.size - result.succeeded.size
        AlertEmailer.notify(alertEmails, s"$failedCount messages failed reprocessing")
      }
    } finally {
      connection.close()
    }
  }
}
