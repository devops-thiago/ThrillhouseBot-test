package inventorysync

import java.net.URI
import java.net.http.{HttpClient => JHttpClient, HttpRequest, HttpResponse}

/** Minimal wrapper around the JDK HTTP client for vendor API calls. */
object HttpClient {
  private val client = JHttpClient.newHttpClient()

  def get(url: String, apiKey: String): String = {
    val request = HttpRequest
      .newBuilder()
      .uri(URI.create(url))
      .header("Authorization", s"Bearer $apiKey")
      .GET()
      .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    response.body()
  }
}

/** Parses raw vendor API JSON responses into domain models. */
object ResponseParser {
  import io.circe.generic.auto._
  import io.circe.parser.decode

  def parsePage(json: String): PageResponse =
    decode[PageResponse](json).fold(
      err => throw new RuntimeException(s"failed to parse vendor response: $err"),
      identity
    )
}
