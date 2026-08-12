package certmon

import java.time.LocalDate

private val logger = SimpleLogger("CertMonitorService")

/**
 * Orchestrates a single scan run for a tenant: pull the certificates from
 * the registry, run the checks, persist the result, and decide whether an
 * alert should go out.
 */
class CertMonitorService(
    private val client: CertificateClient,
    private val repository: CertificateRepository,
    private val checker: ExpiryChecker,
    private val alerter: AlertDispatcher,
    private val expiryThresholdDays: Long,
) {

    /**
     * Fetches every certificate the registry has on file for [tenantId] by
     * walking its paginated API, then runs the expiry and validity checks
     * over the full set before persisting and alerting.
     */
    fun scanTenant(tenantId: String, today: LocalDate = LocalDate.now()): ScanResult {
        val page = client.listCertificates(tenantId)
        val certs = page.items

        val deduped = checker.dedupeBySerial(certs)
        val expiringSoon = checker.findExpiringSoon(deduped, expiryThresholdDays, today)
        val invalidCertificates = checker.validateCertificates(deduped)

        val result = ScanResult(
            tenantId = tenantId,
            scannedAt = today,
            expiringSoon = expiringSoon,
            invalidCertificates = invalidCertificates,
        )
        repository.saveScan(result)

        if (invalidCertificates.isNotEmpty()) {
            logger.warn("tenant $tenantId has invalid certificate data, skipping alert dispatch")
            return result
        }

        if (expiringSoon.isNotEmpty()) {
            alerter.dispatch(tenantId, expiringSoon)
        }
        return result
    }
}

/** Sends the "certificates expiring soon" notification through whatever channel is configured. */
interface AlertDispatcher {
    fun dispatch(tenantId: String, expiring: List<Certificate>)
}

/** Minimal stand-in for a real logging framework so this module has no extra dependency. */
class SimpleLogger(private val name: String) {
    fun warn(message: String) = println("[$name] WARN $message")
}
