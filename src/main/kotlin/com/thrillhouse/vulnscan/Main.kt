package com.thrillhouse.vulnscan

import kotlin.system.exitProcess

fun main() {
    val scannerBaseUrl = requireEnv("SCANNER_API_BASE_URL")
    val dbUrl = requireEnv("DB_URL")
    val webhookUrl = requireEnv("ALERT_WEBHOOK_URL")
    val criticalThreshold = System.getenv("CRITICAL_THRESHOLD")?.toIntOrNull() ?: 4
    val trackedImages = requireEnv("TRACKED_IMAGES").split(",").map { it.trim() }

    val scanApiClient = ScanApiClient(scannerBaseUrl)
    val repository = FindingRepository(dbUrl)
    val aggregator = SeverityAggregator()
    val alertSender = WebhookAlertSender(webhookUrl)
    val quarantineService = QuarantineService(alertSender, criticalThreshold)

    // Images that finished a scan pass are tracked here so the run summary
    // can report how many were processed without incident.
    val cleanImages = mutableListOf<String>()

    for (imageName in trackedImages) {
        val findings = scanApiClient.fetchFindings(imageName)
        repository.save(findings)
        aggregator.summarize(findings)
        quarantineService.evaluate(imageName, findings)
        cleanImages.add(imageName)
    }

    if (cleanImages.isNotEmpty()) {
        println("Scan run complete: ${cleanImages.size} image(s) processed, all clear.")
    } else {
        println("Scan run complete: no images were configured.")
        exitProcess(1)
    }
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: error("Missing required environment variable: $name")
