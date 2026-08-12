package dlq

final case class ProcessOutcome(success: Boolean, detail: String)

trait MessageProcessor {
  def process(messageId: String, payload: String): ProcessOutcome
}

/** Re-publishes the message payload onto the primary work queue. */
final class RepublishProcessor(http: HttpGateway, primaryQueueUrl: String) extends MessageProcessor {
  def process(messageId: String, payload: String): ProcessOutcome = {
    val status = http.post(s"$primaryQueueUrl/messages", payload)
    if (status == 202) ProcessOutcome(success = true, detail = "republished")
    else ProcessOutcome(success = false, detail = s"primary queue rejected with status $status")
  }
}
