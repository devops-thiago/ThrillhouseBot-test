package baggage

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.{Executors, TimeUnit}
import scala.util.control.NonFatal

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.fromEnv()
    val manifests = new ManifestService(new HttpManifestApi(config.manifestApiUrl, config.apiToken))
    val tracker = new TransferTracker(manifests, new StaticHubDirectory(config.transferHubs), LoggingAlertSink)
    val labels = new LabelStore(Paths.get("/var/lib/baggage/labels"))

    val scheduler = Executors.newSingleThreadScheduledExecutor()
    scheduler.scheduleAtFixedRate(
      () =>
        try tracker.reconcileAll()
        catch { case NonFatal(e) => System.err.println(s"reconcile failed: $e") },
      config.reconcileIntervalSeconds,
      config.reconcileIntervalSeconds,
      TimeUnit.SECONDS
    )

    val server = HttpServer.create(new InetSocketAddress(8080), 0)
    server.createContext("/scans", (ex: HttpExchange) => handleScan(ex, tracker))
    server.createContext("/transfers", (ex: HttpExchange) => respond(ex, 200, tracker.upcoming.map(render).mkString("\n")))
    server.createContext("/bags/", (ex: HttpExchange) => handleLabel(ex, labels))
    server.setExecutor(Executors.newFixedThreadPool(8))
    server.start()
  }

  // POST /scans with body "tag,hub,outboundFlight"
  private def handleScan(ex: HttpExchange, tracker: TransferTracker): Unit =
    if (ex.getRequestMethod != "POST") respond(ex, 405, "")
    else {
      val body = new String(ex.getRequestBody.readAllBytes(), UTF_8).trim
      body.split(",").map(_.trim) match {
        case Array(tag, hub, flight) if tag.nonEmpty && hub.nonEmpty && flight.nonEmpty =>
          val state = tracker.recordScan(ScanEvent(tag, hub.toUpperCase, flight, Instant.now()))
          respond(ex, 202, s"${state.tag} scans=${state.scans}")
        case _ => respond(ex, 400, "expected tag,hub,outboundFlight")
      }
    }

  // GET /bags/{tag}/label
  private def handleLabel(ex: HttpExchange, labels: LabelStore): Unit = {
    val tag = ex.getRequestURI.getPath.stripPrefix("/bags/").stripSuffix("/label")
    if (!BagTag.wellFormed(tag)) respond(ex, 400, "bad tag")
    else
      labels.read(tag) match {
        case Some(bytes) =>
          ex.getResponseHeaders.set("Content-Type", "application/pdf")
          ex.sendResponseHeaders(200, bytes.length.toLong)
          ex.getResponseBody.write(bytes)
          ex.close()
        case None => respond(ex, 404, "no label")
      }
  }

  private def render(t: Transfer): String =
    s"${t.tag},${t.outboundFlight},${t.connectionHub},${t.outboundDeparture},${t.status}"

  private def respond(ex: HttpExchange, code: Int, body: String): Unit = {
    val bytes = body.getBytes(UTF_8)
    ex.sendResponseHeaders(code, if (bytes.isEmpty) -1L else bytes.length.toLong)
    if (bytes.nonEmpty) ex.getResponseBody.write(bytes)
    ex.close()
  }
}
