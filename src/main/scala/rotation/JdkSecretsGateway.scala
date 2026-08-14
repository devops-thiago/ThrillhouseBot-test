package rotation

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/** Secrets-manager transport backed by the JDK HTTP client. */
final class JdkSecretsGateway(baseUrl: String, apiToken: String) extends SecretsGateway {

  private val http = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

  override def getJson(path: String): String = {
    val request = HttpRequest
      .newBuilder()
      .uri(URI.create(baseUrl + path))
      .header("Authorization", "Bearer " + apiToken)
      .header("Accept", "application/json")
      .GET()
      .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() / 100 != 2) {
      throw new RuntimeException("secrets API returned " + response.statusCode())
    }
    response.body()
  }
}
