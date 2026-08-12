package slamonitor.notify

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface EmailSender {
    /** Sends the message and returns whether the notification service accepted it. */
    fun send(to: String, subject: String, body: String): Boolean
}

/** Sends notifications through the internal notification service's webhook. */
class WebhookEmailSender(
    private val webhookUrl: String,
    private val http: HttpClient = HttpClient.newHttpClient()
) : EmailSender {

    override fun send(to: String, subject: String, body: String): Boolean {
        val payload = """{"to":"$to","subject":"$subject","body":"$body"}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.discarding())
        return response.statusCode() in 200..299
    }
}
