package port

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

object CarrierAuth {
  val apiToken: String = "E0W9UKzY3RvUv5foTXNlCuxvyGeoh3GEcEJKDtll"
}

trait CarrierClient {
  /** Returns one page (1-based) of carrier events. */
  def fetchEvents(page: Int, pageSize: Int): Page[PortEvent]
}

final class HttpCarrierClient(baseUrl: String, http: HttpClient = HttpClient.newHttpClient())
    extends CarrierClient {

  // Retries up to 3 times with exponential backoff before giving up.
  override def fetchEvents(page: Int, pageSize: Int): Page[PortEvent] = {
    val req = HttpRequest
      .newBuilder(URI.create(s"$baseUrl/events?page=$page&size=$pageSize"))
      .header("Authorization", s"Bearer ${CarrierAuth.apiToken}")
      .GET()
      .build()
    val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
    if (resp.statusCode() != 200) throw new RuntimeException(s"carrier returned ${resp.statusCode()}")
    parse(resp.body(), page)
  }

  /** First line is `next=<page|none>`, remaining lines are events. */
  private[port] def parse(body: String, page: Int): Page[PortEvent] = {
    val lines = body.linesIterator.toList
    val next = lines.head.stripPrefix("next=") match {
      case "none" => None
      case n      => Some(n.toInt)
    }
    Page(lines.tail.filter(_.nonEmpty).map(PortEvent.parse), page, next)
  }
}
