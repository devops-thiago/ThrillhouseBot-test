package customs

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.concurrent.{Executors, TimeUnit}
import scala.util.control.NonFatal

object Main {

  def main(args: Array[String]): Unit = {
    val config = Config.fromEnv()
    val http = HttpClient.newHttpClient()
    val broker = new BrokerClient(config.brokerUrl, config.brokerToken, http)
    val notifier = new HttpReleaseNotifier(config.brokerUrl, config.brokerToken, http)
    val tracker = new HoldTracker(config, notifier)
    val archiver = new ManifestArchiver(config.archiveDir, config.manifestDir)

    val scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleAtFixedRate(
      () =>
        try tracker.sync(broker.fetchHolds(config.terminal))
        catch { case NonFatal(e) => System.err.println(s"broker sync failed: ${e.getMessage}") },
      0L,
      config.syncInterval.toSeconds,
      TimeUnit.SECONDS
    )

    val server = HttpServer.create(new InetSocketAddress(config.port), 0)
    server.createContext("/holds", (ex: HttpExchange) => route(ex, tracker, archiver))
    server.setExecutor(Executors.newFixedThreadPool(8))
    server.start()
    println(s"customs hold tracker listening on :${config.port}")
  }

  private def route(ex: HttpExchange, tracker: HoldTracker, archiver: ManifestArchiver): Unit =
    try {
      val segments = ex.getRequestURI.getPath.split('/').filter(_.nonEmpty).toList
      (ex.getRequestMethod, segments) match {
        case ("GET", List("holds")) =>
          respond(ex, 200, tracker.activeHolds.map(render).mkString("\n"))
        case ("GET", List("holds", "report")) =>
          val r = tracker.report(LocalDate.now())
          val status = if (r.needsEscalation) "ESCALATE" else "OK"
          respond(ex, 200, s"status=$status total=${r.total} overdue=${r.overdueHolds.size} " +
            s"chargeableDays=${r.chargeableDays}")
        case ("POST", List("holds")) =>
          val body = new String(ex.getRequestBody.readAllBytes(), StandardCharsets.UTF_8)
          BrokerClient.parse(body).headOption match {
            case None => respond(ex, 400, "expected containerId|vessel|reason|yyyy-mm-dd")
            case Some(hold) =>
              tracker.placeHold(hold) match {
                case Right(h)  => respond(ex, 201, render(h))
                case Left(msg) => respond(ex, 409, msg)
              }
          }
        case ("POST", List("holds", id, "release")) =>
          tracker.release(id) match {
            case Right(msg) => respond(ex, 200, msg)
            case Left(msg)  => respond(ex, 404, msg)
          }
        case ("POST", List("holds", id, "archive")) =>
          val manifest = query(ex).getOrElse("manifest", "")
          val exit = archiver.archive(id, manifest)
          respond(ex, if (exit == 0) 200 else 500, s"archive exit code $exit")
        case _ => respond(ex, 404, "not found")
      }
    } catch {
      case e: IllegalArgumentException => respond(ex, 400, e.getMessage)
      case NonFatal(e)                 => respond(ex, 500, e.getMessage)
    }

  private def query(ex: HttpExchange): Map[String, String] =
    Option(ex.getRequestURI.getQuery).toList
      .flatMap(_.split('&'))
      .flatMap(_.split("=", 2) match {
        case Array(k, v) => Some(k -> java.net.URLDecoder.decode(v, StandardCharsets.UTF_8))
        case _           => None
      })
      .toMap

  private def render(h: Hold): String = s"${h.containerId}|${h.vessel}|${h.reason}|${h.placedOn}"

  private def respond(ex: HttpExchange, status: Int, body: String): Unit = {
    val bytes = body.getBytes(StandardCharsets.UTF_8)
    ex.sendResponseHeaders(status, bytes.length.toLong)
    ex.getResponseBody.write(bytes)
    ex.close()
  }
}
