package certguard

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

/** Read-only surface the deployment probes and the platform dashboard reads.
  *
  * `/healthz` answers as soon as the process is up; `/status` carries the
  * totals of the last completed sweep, or `pending` before the first one.
  */
final class StatusServer(port: Int) {

  private val latest = new AtomicReference[Option[SweepReport]](None)
  private val server = HttpServer.create(new InetSocketAddress(port), 0)

  server.createContext("/healthz", handler(_ => ujson.Obj("status" -> "ok")))
  server.createContext("/status", handler(_ => statusBody))

  def publish(report: SweepReport): Unit = latest.set(Some(report))

  def start(): Unit = server.start()

  def stop(): Unit = server.stop(0)

  private def statusBody: ujson.Value = latest.get() match {
    case None => ujson.Obj("state" -> "pending")
    case Some(report) =>
      ujson.Obj(
        "state" -> "swept",
        "swept_at" -> report.sweptAt.toString,
        "hosts_examined" -> report.hostsExamined,
        "certificates_seen" -> report.certificatesSeen,
        "suppressed" -> report.suppressed,
        "actionable" -> report.actionable,
        "absent" -> report.absent,
        "lapsed" -> report.lapsed,
        "owners_notified" -> report.ownersNotified
      )
  }

  private def handler(body: HttpExchange => ujson.Value): HttpHandler = new HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val bytes = ujson.write(body(exchange)).getBytes(StandardCharsets.UTF_8)
      exchange.getResponseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(200, bytes.length.toLong)
      val stream = exchange.getResponseBody
      try stream.write(bytes)
      finally stream.close()
    }
  }
}
