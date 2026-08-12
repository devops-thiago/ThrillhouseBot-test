package certmon

import java.sql.DriverManager

/**
 * Entry point: wires up the registry client, repository, and checker, then
 * runs one scan pass over every configured tenant. Meant to be invoked on
 * a schedule (cron, k8s CronJob) rather than run as a long-lived process.
 */
fun main() {
    val registryUrl = requireEnv("CERT_REGISTRY_URL")
    val dbUrl = requireEnv("CERT_DB_URL")
    val tenantIds = requireEnv("CERT_SCAN_TENANT_IDS").split(",").map { it.trim() }
    val thresholdDays = System.getenv("CERT_EXPIRY_THRESHOLD_DAYS")?.toLong() ?: 30L

    val connection = DriverManager.getConnection(dbUrl).apply { autoCommit = false }
    val client = HttpCertificateClient(registryUrl)
    val repository = CertificateRepository(connection)
    val checker = ExpiryChecker()
    val alerter = ConsoleAlertDispatcher()
    val service = CertMonitorService(client, repository, checker, alerter, thresholdDays)

    for (tenantId in tenantIds) {
        runCatching { service.scanTenant(tenantId) }
            .onFailure { err -> println("scan failed for tenant $tenantId: ${err.message}") }
    }

    connection.close()
}

private fun requireEnv(name: String): String =
    System.getenv(name) ?: error("missing required environment variable $name")

/** Alert dispatcher that just prints to stdout; a real deployment would wire in Slack/email. */
class ConsoleAlertDispatcher : AlertDispatcher {
    override fun dispatch(tenantId: String, expiring: List<Certificate>) {
        println("tenant $tenantId has ${expiring.size} certificate(s) expiring soon:")
        for (cert in expiring) {
            println("  ${cert.commonName} (serial ${cert.serial}) expires ${cert.notAfter}")
        }
    }
}
