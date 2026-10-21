package coldchain

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

trait AlertGateway {

  /** Delivers an alert. Returns Left("no recipients") when the recipient list is empty, and never
    * reports success unless a message was actually handed to the mail relay.
    */
  def send(recipients: List[String], subject: String, body: String): Either[String, Unit]
}

class HttpAlertGateway(endpoint: String) extends AlertGateway {
  private val client = HttpClient.newHttpClient()

  override def send(recipients: List[String], subject: String, body: String): Either[String, Unit] =
    if (recipients.isEmpty) Left("no recipients")
    else {
      val payload = recipients.mkString(",") + "\n" + subject + "\n" + body
      val request = HttpRequest
        .newBuilder(URI.create(endpoint))
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build()
      val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
      if (status / 100 == 2) Right(()) else Left(s"relay returned $status")
    }
}

class AlertDispatcher(gateway: AlertGateway, recipients: List[String]) {
  def dispatch(e: Excursion): Either[String, Unit] =
    gateway.send(recipients, s"Cold-chain excursion on ${e.shipmentId}", Report.summarise(e))
}
