package slamonitor

import slamonitor.client.HttpTicketClient
import slamonitor.evaluator.SlaEvaluator
import slamonitor.evaluator.TicketDeduper
import slamonitor.model.DefaultPolicies
import slamonitor.notify.AlertDispatcher
import slamonitor.notify.PriorityAwareRecipientResolver
import slamonitor.notify.WebhookEmailSender
import kotlin.system.exitProcess

fun main() {
    val baseUrl = System.getenv("HELPDESK_API_BASE_URL")
        ?: run {
            System.err.println("HELPDESK_API_BASE_URL is required")
            exitProcess(1)
        }
    val webhookUrl = System.getenv("NOTIFICATION_WEBHOOK_URL")
        ?: run {
            System.err.println("NOTIFICATION_WEBHOOK_URL is required")
            exitProcess(1)
        }
    val pollIntervalSeconds = System.getenv("SLA_POLL_INTERVAL_SECONDS")?.toLong() ?: 300L
    val recipients = System.getenv("ALERT_EMAIL_RECIPIENTS")?.split(",")?.map { it.trim() } ?: emptyList()

    val ticketClient = HttpTicketClient(baseUrl)
    val evaluator = SlaEvaluator(DefaultPolicies.ALL)
    val deduper = TicketDeduper()
    val dispatcher = AlertDispatcher(
        emailSender = WebhookEmailSender(webhookUrl),
        recipientResolver = PriorityAwareRecipientResolver(onCallManagers = emptyList())
    )

    while (true) {
        val tickets = deduper.filterNew(ticketClient.fetchOpenTickets())
        val breaches = evaluator.findBreaches(tickets)
        val topBreaches = evaluator.mostOverdue(breaches, limit = 5)

        val failed = dispatcher.dispatch(topBreaches, recipients)
        if (failed.isNotEmpty()) {
            println("WARNING: failed to notify ${failed.size} recipient(s): ${failed.joinToString()}")
        }

        Thread.sleep(pollIntervalSeconds * 1000)
    }
}
