package certguard

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/** Thin JSON-over-HTTP helper shared by the registry, CA and relay adapters.
  *
  * Everything certguard talks to is a first-party service behind the mesh, so
  * there is no retry policy here on purpose: a failed sweep is logged and the
  * next tick tries again a few minutes later.
  */
final class HttpJson(token: String, requestTimeout: Duration = Duration.ofSeconds(30)) {

  private val client = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

  def get(uri: String): ujson.Value = {
    val request = authorized(HttpRequest.newBuilder(URI.create(uri))).GET().build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      throw new IllegalStateException(s"GET $uri answered ${response.statusCode()}")
    }
    ujson.read(response.body())
  }

  /** Posts `payload` and reports whether the peer accepted it. */
  def post(uri: String, payload: ujson.Value): Boolean = {
    val request = authorized(HttpRequest.newBuilder(URI.create(uri)))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(ujson.write(payload)))
      .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    response.statusCode() < 300
  }

  private def authorized(builder: HttpRequest.Builder): HttpRequest.Builder = {
    val timed = builder.timeout(requestTimeout).header("Accept", "application/json")
    if (token.isEmpty) timed else timed.header("Authorization", s"Bearer $token")
  }
}
