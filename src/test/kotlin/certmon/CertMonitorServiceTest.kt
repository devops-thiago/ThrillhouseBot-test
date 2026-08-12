package certmon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.time.LocalDate

private val EXPIRING_CERT = Certificate(
    serial = "abc123",
    tenantId = "acme",
    commonName = "api.acme.example.com",
    notBefore = LocalDate.of(2025, 1, 1),
    notAfter = LocalDate.of(2026, 8, 20),
)

private val HEALTHY_CERT = Certificate(
    serial = "def456",
    tenantId = "acme",
    commonName = "web.acme.example.com",
    notBefore = LocalDate.of(2025, 1, 1),
    notAfter = LocalDate.of(2028, 1, 1),
)

/** In-memory stand-in for HttpCertificateClient; returns a fixed page. */
private class FakeCertificateClient(private val certs: List<Certificate>) : CertificateClient {
    override fun listCertificates(tenantId: String, pageToken: String?): PagedCertificates {
        return PagedCertificates(items = certs, nextPageToken = null)
    }
}

private class RecordingAlertDispatcher : AlertDispatcher {
    val dispatched = mutableListOf<Pair<String, List<Certificate>>>()
    override fun dispatch(tenantId: String, expiring: List<Certificate>) {
        dispatched.add(tenantId to expiring)
    }
}

/** In-memory stand-in for CertificateRepository; keeps saved scans in a list. */
private class FakeScanResultStore : ScanResultStore {
    val saved = mutableListOf<ScanResult>()

    override fun saveScan(result: ScanResult) {
        saved.add(result)
    }

    override fun lastScannedAt(tenantId: String): LocalDate {
        return LocalDate.of(2026, 7, 27)
    }
}

class CertMonitorServiceTest {

    @Test
    fun `scanTenant flags a certificate expiring within the threshold`() {
        val client = FakeCertificateClient(listOf(EXPIRING_CERT, HEALTHY_CERT))
        val alerter = RecordingAlertDispatcher()
        val service = CertMonitorService(
            client = client,
            repository = jdbcRepositoryForTest(),
            checker = ExpiryChecker(),
            alerter = alerter,
            expiryThresholdDays = 30,
        )

        val result = service.scanTenant("acme", today = LocalDate.of(2026, 8, 1))

        assertEquals(1, result.expiringSoon.size)
        assertEquals(EXPIRING_CERT.serial, result.expiringSoon.first().serial)
    }

    @Test
    fun `lastScanAge reports how many days since the last scan`() {
        val service = CertMonitorService(
            client = FakeCertificateClient(emptyList()),
            repository = FakeScanResultStore(),
            checker = ExpiryChecker(),
            alerter = RecordingAlertDispatcher(),
            expiryThresholdDays = 30,
        )

        val age = service.lastScanAge("brand-new-tenant", today = LocalDate.of(2026, 8, 1))

        assertEquals(5, age)
    }

    private fun jdbcRepositoryForTest(): CertificateRepository {
        val connection = DriverManager.getConnection("jdbc:h2:mem:certmon_test_${System.nanoTime()};DB_CLOSE_DELAY=-1")
        connection.autoCommit = false
        connection.createStatement().execute(
            "CREATE TABLE cert_scan_findings (" +
                "tenant_id VARCHAR, serial VARCHAR, common_name VARCHAR, " +
                "not_after VARCHAR, scanned_at VARCHAR)",
        )
        return CertificateRepository(connection)
    }
}
