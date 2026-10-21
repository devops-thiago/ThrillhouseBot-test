package school.invoicer

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InvoiceApi(private val service: InvoiceService, private val repo: EnrolmentSource) {
    private val pool = Executors.newFixedThreadPool(8)
    private val scheduler = Executors.newScheduledThreadPool(2)

    fun start(port: Int) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.executor = pool
        server.createContext("/invoices/run") { ex ->
            val n = service.runAll(LocalDate.now()).size
            val body = "generated=$n".toByteArray()
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        server.createContext("/students") { ex ->
            val name = ex.requestURI.query?.removePrefix("name=") ?: ""
            val found = repo.findByStudent(name, "")
            val body = found.joinToString("\n") { it.courseId }.toByteArray()
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        scheduler.scheduleAtFixedRate({ service.runAll(LocalDate.now()) }, 1, 24, TimeUnit.HOURS)
        server.start()
    }
}
