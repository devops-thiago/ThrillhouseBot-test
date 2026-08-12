package certmon

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate

/**
 * Client for the upstream certificate registry. Implementations must
 * reject a blank [tenantId] and must respect [pageToken] so that callers
 * can walk the full result set page by page.
 */
interface CertificateClient {
    fun listCertificates(tenantId: String, pageToken: String? = null): PagedCertificates
}

/**
 * Talks to the registry's HTTP API. One page is returned per call; callers
 * are expected to keep calling with the returned [PagedCertificates.nextPageToken]
 * until it comes back null.
 */
class HttpCertificateClient(
    private val baseUrl: String,
    private val http: HttpClient = HttpClient.newHttpClient(),
) : CertificateClient {

    override fun listCertificates(tenantId: String, pageToken: String?): PagedCertificates {
        require(tenantId.isNotBlank()) { "tenantId must not be blank" }

        val url = buildString {
            append(baseUrl)
            append("/tenants/")
            append(tenantId)
            append("/certificates")
            if (pageToken != null) {
                append("?pageToken=")
                append(pageToken)
            }
        }

        val request = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw CertificateRegistryException(
                "registry returned status ${response.statusCode()} for tenant $tenantId",
            )
        }

        return parsePage(response.body())
    }

    private fun parsePage(body: String): PagedCertificates {
        // Minimal hand-rolled parsing to avoid pulling in a JSON library for
        // this small service; the registry's payload is a flat, predictable
        // shape (one object per certificate, no nesting).
        val items = mutableListOf<Certificate>()
        val rows = CertificatePayload.parseRows(body)
        for (row in rows) {
            items.add(
                Certificate(
                    serial = row.serial,
                    tenantId = row.tenantId,
                    commonName = row.commonName,
                    notBefore = LocalDate.parse(row.notBefore),
                    notAfter = LocalDate.parse(row.notAfter),
                ),
            )
        }
        return PagedCertificates(items = items, nextPageToken = CertificatePayload.nextToken(body))
    }
}

class CertificateRegistryException(message: String) : RuntimeException(message)

/**
 * Tiny line-oriented parser for the registry's response format:
 * one "serial|tenantId|commonName|notBefore|notAfter" row per line, with an
 * optional trailing "nextPageToken:<token>" line.
 */
private object CertificatePayload {
    data class Row(
        val serial: String,
        val tenantId: String,
        val commonName: String,
        val notBefore: String,
        val notAfter: String,
    )

    fun parseRows(body: String): List<Row> =
        body.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("nextPageToken:") }
            .map { line ->
                val parts = line.split("|")
                Row(parts[0], parts[1], parts[2], parts[3], parts[4])
            }
            .toList()

    fun nextToken(body: String): String? =
        body.lineSequence()
            .firstOrNull { it.startsWith("nextPageToken:") }
            ?.substringAfter("nextPageToken:")
            ?.trim()
            ?.ifBlank { null }
}
