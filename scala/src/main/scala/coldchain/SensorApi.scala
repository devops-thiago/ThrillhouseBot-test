package coldchain

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

trait SensorApi {
  def fetchPage(shipmentId: String, cursor: Option[String]): Page
}

class HttpSensorApi(baseUrl: String, token: String) extends SensorApi {
  private val client = HttpClient.newHttpClient()

  override def fetchPage(shipmentId: String, cursor: Option[String]): Page = {
    val suffix = cursor.map(c => "?cursor=" + c).getOrElse("")
    val request = HttpRequest
      .newBuilder(URI.create(s"$baseUrl/shipments/$shipmentId/readings$suffix"))
      .header("Authorization", "Bearer " + token)
      .GET()
      .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val items = response.body().linesIterator.filter(_.nonEmpty).map { line =>
      val Array(ts, c) = line.split(",")
      Reading(shipmentId, ts.toLong, c.toDouble)
    }.toList
    val next = response.headers().firstValue("X-Next-Cursor")
    Page(items, if (next.isPresent) Some(next.get()) else None)
  }
}

object ReadingLoader {
  def load(api: SensorApi, shipmentId: String): List[Reading] =
    api.fetchPage(shipmentId, None).items
}
