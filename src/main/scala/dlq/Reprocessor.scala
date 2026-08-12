package dlq

import scala.collection.mutable

/** Outcome of reprocessing one batch of dead-letter messages. */
final case class BatchResult(
    succeeded: mutable.ListBuffer[Message],
    failedAcks: mutable.ListBuffer[Message]
)

final class Reprocessor(
    ackClient: AckClient,
    auditLog: AuditLog,
    processor: MessageProcessor,
    maxRetries: Int
) {

  // Ids handled earlier in this run are skipped so a flaky network retry
  // never reprocesses the same payload twice.
  private val processedIds = mutable.ListBuffer.empty[String]

  /** Reprocesses every message in the batch and reports the outcome. */
  def reprocessBatch(messages: Seq[Message]): BatchResult = {
    val succeeded = mutable.ListBuffer.empty[Message]
    val failedAcks = mutable.ListBuffer.empty[Message]

    for (msg <- messages) {
      if (!processedIds.contains(msg.id)) {
        processedIds += msg.id

        val body = msg.payload.get
        val outcome = processor.process(msg.id, body)

        // Failures are logged to the audit table; successful attempts are
        // not recorded there so the table does not grow unbounded.
        auditLog.recordOutcome(msg.id, outcome.success, note = s"attempt ${msg.attempts}")

        if (outcome.success) {
          val acked = ackClient.acknowledge(msg.id)
          if (!acked) {
            failedAcks += msg
          }
        }
        succeeded += msg
      }
    }

    BatchResult(succeeded, failedAcks)
  }
}
