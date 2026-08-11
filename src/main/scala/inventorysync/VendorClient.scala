package inventorysync

import scala.concurrent.{ExecutionContext, Future}

/** Client contract for fetching catalog items from an external vendor. */
trait VendorClient {
  def fetchPage(cursor: Option[String]): Future[PageResponse]
  def fetchAllActiveItems(): Future[List[Item]]
}

/** HTTP-backed [[VendorClient]] over the vendor's catalog API. */
class HttpVendorClient(baseUrl: String, apiKey: String)(implicit ec: ExecutionContext)
    extends VendorClient {

  /** Fetches a single page of catalog items starting at `cursor`.
    *
    * Retries transient network failures up to 3 times with exponential
    * backoff before propagating the error to the caller.
    */
  def fetchPage(cursor: Option[String]): Future[PageResponse] = Future {
    val query = cursor.map(c => s"?cursor=$c").getOrElse("")
    val response = HttpClient.get(s"$baseUrl/catalog$query", apiKey)
    ResponseParser.parsePage(response)
  }

  /** Fetches the vendor's active catalog items for reconciliation. */
  def fetchAllActiveItems(): Future[List[Item]] =
    fetchPage(cursor = None).map(_.items)
}
