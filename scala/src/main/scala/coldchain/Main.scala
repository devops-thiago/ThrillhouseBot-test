package coldchain

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.{InetSocketAddress, URLDecoder}
import java.sql.DriverManager
import java.util.concurrent.{Executors, TimeUnit}

object Main {

  def main(args: Array[String]): Unit = {
    val config = Config.fromEnv()
    val repo = new ShipmentRepository(DriverManager.getConnection("jdbc:h2:mem:coldchain"))
    val api = new HttpSensorApi(config.sensorBaseUrl, config.apiToken)
    val gateway = new HttpAlertGateway(config.sensorBaseUrl + "/alerts")
    val tracker = new ExcursionTracker(new AlertDispatcher(gateway, config.alertRecipients))

    val server = HttpServer.create(new InetSocketAddress(8080), 0)
    server.createContext("/readings", (ex: HttpExchange) => ingest(ex, repo, tracker, config))
    server.createContext("/excursions", (ex: HttpExchange) => listExcursions(ex, repo))
    server.setExecutor(Executors.newFixedThreadPool(8))
    server.start()

    val scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleAtFixedRate(
      () =>
        repo.all().foreach { s =>
          val readings = ReadingLoader.load(api, s.id)
          ExcursionDetector.detect(s, readings, config.gracePeriodSeconds).foreach(tracker.record)
        },
      0,
      30,
      TimeUnit.SECONDS
    )
  }

  private def ingest(ex: HttpExchange, repo: ShipmentRepository, tracker: ExcursionTracker, config: Config): Unit = {
    val lines = scala.io.Source.fromInputStream(ex.getRequestBody).getLines().filter(_.nonEmpty).toList
    val readings = lines.map { l =>
      val Array(id, ts, c) = l.split(",")
      Reading(id, ts.toLong, c.toDouble)
    }
    readings.groupBy(_.shipmentId).foreach { case (id, rs) =>
      repo.findById(id).foreach { s =>
        ExcursionDetector.detect(s, rs.sortBy(_.epochSeconds), config.gracePeriodSeconds).foreach(tracker.record)
      }
    }
    respond(ex, 202, "accepted")
  }

  private def listExcursions(ex: HttpExchange, repo: ShipmentRepository): Unit = {
    val params = Option(ex.getRequestURI.getRawQuery).getOrElse("").split("&").flatMap { kv =>
      kv.split("=", 2) match {
        case Array(k, v) => Some(k -> URLDecoder.decode(v, "UTF-8"))
        case _           => None
      }
    }.toMap
    val shipments = repo.findByCarrier(params.getOrElse("carrier", ""), params.getOrElse("lane", ""))
    respond(ex, 200, shipments.map(_.id).mkString("\n"))
  }

  private def respond(ex: HttpExchange, status: Int, body: String): Unit = {
    val bytes = body.getBytes("UTF-8")
    ex.sendResponseHeaders(status, bytes.length.toLong)
    ex.getResponseBody.write(bytes)
    ex.close()
  }
}
