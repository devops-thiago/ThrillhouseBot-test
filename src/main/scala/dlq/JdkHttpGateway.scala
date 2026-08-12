package dlq

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

/** Talks to the DLQ HTTP API using the JDK's built-in HTTP client. */
final class JdkHttpGateway extends HttpGateway {
  private val client = HttpClient.newHttpClient()

  def getPage(url: String, cursor: Option[String]): MessagePage = {
    val target = cursor.fold(url)(c => s"$url?cursor=$c")
    val request = HttpRequest.newBuilder(URI.create(target)).GET().build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    JsonCodec.parsePage(response.body())
  }

  def post(url: String, body: String = ""): Int = {
    val request = HttpRequest
      .newBuilder(URI.create(url))
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()
    client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
  }
}
