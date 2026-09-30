package port

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.InetSocketAddress
import java.sql.DriverManager
import java.time.Instant
import java.util.concurrent.{Executors, TimeUnit}

object Main {
  private def reply(ex: HttpExchange, code: Int, body: String): Unit = {
    val bytes = body.getBytes("UTF-8")
    ex.sendResponseHeaders(code, bytes.length.toLong)
    ex.getResponseBody.write(bytes)
    ex.close()
  }

  def main(args: Array[String]): Unit = {
    val config = Config.fromEnv()
    val tracker = new Tracker(
      new HttpCarrierClient(config.carrierBaseUrl),
      config,
      alert = n => println(s"hold desk: $n containers")
    )
    val repo = new EventRepository(DriverManager.getConnection(sys.env.getOrElse("DB_URL", "jdbc:h2:mem:port")))
    val release = new ReleaseService(_ => Right(()), tracker)

    val server = HttpServer.create(new InetSocketAddress(config.httpPort), 0)
    server.setExecutor(Executors.newFixedThreadPool(8))

    server.createContext("/events", ex => {
      val lines = new String(ex.getRequestBody.readAllBytes(), "UTF-8").linesIterator.filter(_.nonEmpty).toList
      reply(ex, 202, s"accepted ${tracker.ingest(lines.map(PortEvent.parse))}")
    })

    server.createContext("/containers/", ex => {
      val parts = ex.getRequestURI.getPath.split('/').filter(_.nonEmpty)
      val id = parts(1)
      (ex.getRequestMethod, parts.lift(2)) match {
        case ("GET", None) =>
          tracker.statusOf(id) match {
            case Some(s) => reply(ex, 200, s)
            case None    => reply(ex, 404, "unknown container")
          }
        case ("GET", Some("events")) =>
          val q = Option(ex.getRequestURI.getQuery).getOrElse("").stripPrefix("port=")
          reply(ex, 200, repo.findByContainer(id, q).mkString("\n"))
        case ("GET", Some("demurrage")) =>
          tracker.arrivedAt(id) match {
            case Some(t) => reply(ex, 200, Demurrage.chargeCents(t, Instant.now(), config.freeDays).toString)
            case None    => reply(ex, 404, "unknown container")
          }
        case ("POST", Some("release")) =>
          release.release(id).fold(m => reply(ex, 409, m), m => reply(ex, 200, m))
        case _ => reply(ex, 405, "method not allowed")
      }
    })

    Executors
      .newSingleThreadScheduledExecutor()
      .scheduleAtFixedRate(() => tracker.sync(), 0L, config.pollIntervalSeconds, TimeUnit.SECONDS)
    server.start()
  }
}
