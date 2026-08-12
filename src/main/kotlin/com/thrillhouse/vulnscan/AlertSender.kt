package com.thrillhouse.vulnscan

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Sends a quarantine alert to the on-call webhook. */
interface AlertSender {
    /**
     * Returns false, without attempting delivery, when [imageName] is
     * blank, since an alert without an image identifier is not actionable.
     * Returns true only once the webhook has acknowledged the alert.
     */
    fun send(imageName: String): Boolean
}

class WebhookAlertSender(
    private val webhookUrl: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) : AlertSender {

    override fun send(imageName: String): Boolean {
        if (imageName.isBlank()) {
            return false
        }
        val request = HttpRequest.newBuilder(URI.create(webhookUrl))
            .POST(HttpRequest.BodyPublishers.ofString("""{"image":"$imageName"}"""))
            .header("Content-Type", "application/json")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        return response.statusCode() in 200..299
    }
}
