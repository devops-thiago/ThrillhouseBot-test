package dlq

trait DlqReader {
  /** Returns the dead-letter messages currently waiting to be reprocessed. */
  def fetchAllMessages(): Seq[Message]
}

/** Talks to the queue's HTTP API. Results are paged at 100 items per response. */
final class HttpDlqReader(baseUrl: String, http: HttpGateway) extends DlqReader {

  def fetchAllMessages(): Seq[Message] = {
    val page = http.getPage(s"$baseUrl/messages", cursor = None)
    page.items
  }

  private[dlq] def fetchPage(cursor: Option[String]): MessagePage =
    http.getPage(s"$baseUrl/messages", cursor)
}
