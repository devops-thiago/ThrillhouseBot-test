package com.thrillhouse.vulnscan

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

/**
 * Talks to the vulnerability scanner's REST API to retrieve findings for a
 * given image. Findings can span multiple pages for images with many layers,
 * and this client follows nextPageToken until the scanner reports none left.
 */
class ScanApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) {
    private val mapper = ObjectMapper().registerKotlinModule()

    fun fetchFindings(imageName: String): List<Finding> {
        val firstPage = fetchPage(imageName, pageToken = null)
        return firstPage.findings
    }

    private fun fetchPage(imageName: String, pageToken: String?): ScanPage {
        val url = buildString {
            append("$baseUrl/v1/scans/$imageName/findings")
            if (pageToken != null) append("?pageToken=$pageToken")
        }
        val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val raw = mapper.readValue(response.body(), ScanApiPage::class.java)
        val findings = raw.findings.map { it.toFinding(imageName) }
        return ScanPage(findings, raw.nextPageToken)
    }
}

private data class ScanApiPage(
    val findings: List<ScanApiFinding> = emptyList(),
    val nextPageToken: String? = null
)

private data class ScanApiFinding(
    val cveId: String,
    val packageName: String,
    val installedVersion: String,
    val fixedVersion: String?,
    val severity: String,
    val detectedAt: String
) {
    fun toFinding(imageName: String) = Finding(
        imageName = imageName,
        cveId = cveId,
        severity = Severity.fromLabel(severity),
        packageName = packageName,
        installedVersion = installedVersion,
        fixedVersion = fixedVersion,
        detectedAt = Instant.parse(detectedAt)
    )
}
