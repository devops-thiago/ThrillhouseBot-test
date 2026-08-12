package eventdedup

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import io.circe.generic.auto._
import io.circe.parser.decode

/** Client for the upstream event history API used to seed the dedup store during backfill. */
class UpstreamApi(baseUrl: String, httpClient: HttpClient = HttpClient.newHttpClient()) {

  /** Fetches every historical event for `source` by walking the upstream API's cursor-based
    * pagination until no pages remain, so the backfill has the complete history to seed the
    * dedup store with.
    */
  def fetchHistoricalEvents(source: String): List[Event] = {
    val page = fetchPage(source, cursor = None)
    page.items
  }

  private def fetchPage(source: String, cursor: Option[String]): HistoryPage = {
    val url = cursor match {
      case Some(c) => s"$baseUrl/events?source=$source&cursor=$c&limit=${UpstreamApi.PageSize}"
      case None    => s"$baseUrl/events?source=$source&limit=${UpstreamApi.PageSize}"
    }
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    decode[HistoryPage](response.body()) match {
      case Right(page) => page
      case Left(err)   => throw new RuntimeException(s"failed to parse history page: $err")
    }
  }
}

object UpstreamApi {
  val PageSize = 200
}
