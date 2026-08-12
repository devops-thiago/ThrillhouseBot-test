package dlq

/** Minimal HTTP abstraction so callers can be tested without a real socket. */
trait HttpGateway {
  def getPage(url: String, cursor: Option[String]): MessagePage
  def post(url: String, body: String = ""): Int
}
