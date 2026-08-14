package rotation

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/** Ticketing client backed by the JDK HTTP client. */
final class JdkTicketClient(baseUrl: String, apiToken: String) extends TicketClient {

  private val http = HttpClient
    .newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

  override def openTicket(secret: Secret): Option[String] = {
    val payload = "{\"secret\":\"" + secret.name + "\",\"team\":\"" + secret.owner +
      "\",\"kind\":\"credential-rotation\"}"
    val request = HttpRequest
      .newBuilder()
      .uri(URI.create(baseUrl + "/tickets"))
      .header("Authorization", "Bearer " + apiToken)
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(payload))
      .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    response.statusCode() match {
      case 409                               => None
      case code if code >= 200 && code < 300 => JsonCodec.ticketId(response.body())
      case code => throw new RuntimeException("ticket API returned " + code)
    }
  }
}
