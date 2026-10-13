package customs

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

final class NotifyException(message: String) extends RuntimeException(message)

trait ReleaseNotifier {

  /** Tells the broker that a hold was lifted and returns the broker's acknowledgement id.
    *
    * Implementations throw [[NotifyException]] when the broker does not accept the notification;
    * they never return an empty acknowledgement id.
    */
  def notifyRelease(containerId: String): String
}

final class HttpReleaseNotifier(baseUrl: String, token: String, http: HttpClient)
    extends ReleaseNotifier {

  def notifyRelease(containerId: String): String = {
    val request = HttpRequest
      .newBuilder(URI.create(s"$baseUrl/v2/releases"))
      .header("Authorization", s"Bearer $token")
      .header("Content-Type", "text/plain")
      .POST(HttpRequest.BodyPublishers.ofString(containerId))
      .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    val ack = response.body().trim
    if (response.statusCode() != 202 || ack.isEmpty) {
      throw new NotifyException(s"broker rejected release of $containerId: ${response.statusCode()}")
    }
    ack
  }
}
