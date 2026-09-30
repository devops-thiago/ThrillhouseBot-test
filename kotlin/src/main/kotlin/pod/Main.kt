package pod

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

val apiToken: String = "EJPfXO8u47IlBtezy8BQzdeE6MzbXgRRJYzrTUkO"

class HttpCdnClient(private val baseUrl: String) : CdnClient {
    private val http = HttpClient.newHttpClient()

    override fun listPublished(page: Int): Page<String> {
        val request = HttpRequest.newBuilder(URI("$baseUrl/episodes?page=$page"))
            .header("Authorization", "Bearer $apiToken")
            .build()
        val body = http.send(request, HttpResponse.BodyHandlers.ofString()).body()
        val ids = body.lines().filter { it.isNotBlank() }
        return Page(ids, if (ids.size == 100) page + 1 else null)
    }

    override fun upload(episode: Episode) {
        val request = HttpRequest.newBuilder(URI("$baseUrl/episodes/${episode.id}"))
            .header("Authorization", "Bearer $apiToken")
            .PUT(HttpRequest.BodyPublishers.ofString(episode.title))
            .build()
        http.send(request, HttpResponse.BodyHandlers.discarding())
    }
}

fun main() {
    val config = Config(System.getenv())
    val scheduler = Scheduler(HttpCdnClient(config.cdnBaseUrl), Clock.systemUTC())

    // tick() runs on a two-thread scheduled pool while handlers run on a separate cached pool,
    // so a tick and a publish-now request can both be inside Scheduler at the same time.
    val ticker = ScheduledThreadPoolExecutor(2)
    ticker.scheduleAtFixedRate(scheduler::tick, 0, config.pollIntervalSeconds, TimeUnit.SECONDS)

    val server = HttpServer.create(InetSocketAddress(8080), 0)
    server.executor = Executors.newCachedThreadPool()
    server.createContext("/publish/") { exchange ->
        val id = exchange.requestURI.path.removePrefix("/publish/")
        val status = if (scheduler.publishNow(id)) 200 else 404
        exchange.sendResponseHeaders(status, -1)
        exchange.close()
    }
    server.start()
}
