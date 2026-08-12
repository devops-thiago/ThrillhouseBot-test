package webhooks

/** Result of attempting to deliver a payload to a subscriber's HTTP endpoint. */
final case class SendResult(isSuccess: Boolean, statusCode: Option[Int])

/** Sends webhook payloads over HTTP. */
trait HttpSender {
  def send(url: String, payload: String): SendResult
}
