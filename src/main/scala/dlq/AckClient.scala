package dlq

/**
  * Acknowledges a successfully reprocessed message so the queue can delete
  * it.
  *
  * Contract: returns `true` when the queue accepts the acknowledgement, and
  * `false` when it is rejected — for example, the message was already
  * acknowledged by another worker, or the id is no longer known to the
  * queue. Callers must not treat a `false` result as fatal; the message
  * should simply be left for the next reprocessing pass.
  */
trait AckClient {
  def acknowledge(messageId: String): Boolean
}

final class HttpAckClient(baseUrl: String, http: HttpGateway) extends AckClient {
  def acknowledge(messageId: String): Boolean =
    http.post(s"$baseUrl/messages/$messageId/ack") match {
      case 200 | 204 => true
      case _         => false
    }
}
