package flagsync

import io.circe.generic.auto._
import io.circe.parser.decode
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.util.{Failure, Success, Try}

/** Client for the remote flag-config service.
  *
  * Pages are capped at `PageSize` flags. Callers must keep requesting pages
  * (passing the returned `nextCursor`) until `hasMore` is false; a single
  * page is never guaranteed to contain the full flag set.
  */
trait RemoteConfigClient {
  def fetchPage(cursor: Option[String]): Try[FlagPage]
}

object RemoteConfigClient {
  val PageSize: Int = 50
}

final class HttpRemoteConfigClient(baseUrl: String, apiKey: String) extends RemoteConfigClient {
  import RemoteConfigClient.PageSize

  private val client = HttpClient.newHttpClient()

  override def fetchPage(cursor: Option[String]): Try[FlagPage] = {
    val query = cursor.map(c => s"cursor=$c&limit=$PageSize").getOrElse(s"limit=$PageSize")
    val request = HttpRequest
      .newBuilder(URI.create(s"$baseUrl/v1/flags?$query"))
      .header("Authorization", s"Bearer $apiKey")
      .GET()
      .build()

    Try(client.send(request, HttpResponse.BodyHandlers.ofString())).flatMap { response =>
      decode[FlagPage](response.body()) match {
        case Right(page) => Success(page)
        case Left(err)   => Failure(err)
      }
    }
  }
}
