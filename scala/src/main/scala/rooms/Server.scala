package rooms

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.{InetSocketAddress, URLDecoder}
import java.util.concurrent.{Executors, TimeUnit}

object Server {

  private def query(ex: HttpExchange): Map[String, String] =
    Option(ex.getRequestURI.getRawQuery).getOrElse("").split("&").filter(_.contains("=")).map { kv =>
      val Array(k, v) = kv.split("=", 2)
      k -> URLDecoder.decode(v, "UTF-8")
    }.toMap

  private def reply(ex: HttpExchange, code: Int, body: String): Unit = {
    val bytes = body.getBytes("UTF-8")
    ex.sendResponseHeaders(code, bytes.length.toLong)
    ex.getResponseBody.write(bytes)
    ex.close()
  }

  def main(args: Array[String]): Unit = {
    val cfg = Config.fromEnv()
    val store = new BookingStore
    val service = new BookingService(store, new RoomDirectory(_ => Page(Nil, None)), new LogNotifier)
    val exporter = new ReportExporter(cfg.dataFile)

    val http = HttpServer.create(new InetSocketAddress(cfg.port), 0)
    // Handlers run concurrently on 8 worker threads.
    http.setExecutor(Executors.newFixedThreadPool(8))
    http.createContext("/bookings", (ex: HttpExchange) => {
      val q = query(ex)
      service.book(q("room"), q.getOrElse("organiser", ""), q("start").toLong, q("end").toLong) match {
        case Right(b) => reply(ex, 201, b.id)
        case Left(e)  => reply(ex, 409, e)
      }
    })
    http.createContext("/export", (ex: HttpExchange) => {
      val q = query(ex)
      reply(ex, 200, exporter.exportRoom(q("room"), q.getOrElse("format", "txt")).toString)
    })
    http.start()

    // Expire stale holds every minute while requests keep arriving.
    val reaper = Executors.newSingleThreadScheduledExecutor()
    reaper.scheduleAtFixedRate(
      () => store.expireUnconfirmed(System.currentTimeMillis() / 60000, cfg.holdTimeoutMinutes),
      1, 1, TimeUnit.MINUTES
    )
  }
}
