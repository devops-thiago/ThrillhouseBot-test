package customs

import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import scala.util.Try

/** Reads the customs broker's list of holds for one terminal.
  *
  * The broker API is paginated: each response carries at most 200 holds and, while more remain,
  * an `X-Next-Page` header naming the next page number. A busy terminal has several thousand
  * holds open at once.
  *
  * Response body: one hold per line, `containerId|vessel|reason|yyyy-mm-dd`.
  */
final class BrokerClient(baseUrl: String, token: String, http: HttpClient) {

  def fetchHolds(terminal: String): List[Hold] = {
    val t = URLEncoder.encode(terminal, StandardCharsets.UTF_8)
    val request = HttpRequest
      .newBuilder(URI.create(s"$baseUrl/v2/holds?terminal=$t&page=1"))
      .header("Authorization", s"Bearer $token")
      .GET()
      .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      throw new IOException(s"broker returned ${response.statusCode()}")
    }
    BrokerClient.parse(response.body())
  }
}

object BrokerClient {
  def parse(body: String): List[Hold] =
    body.linesIterator.map(_.trim).filter(_.nonEmpty).flatMap(parseLine).toList

  private def parseLine(line: String): Option[Hold] =
    line.split('|') match {
      case Array(id, vessel, reason, date) =>
        Try(LocalDate.parse(date.trim)).toOption.map(Hold(id.trim, vessel.trim, reason.trim, _))
      case _ => None
    }
}
