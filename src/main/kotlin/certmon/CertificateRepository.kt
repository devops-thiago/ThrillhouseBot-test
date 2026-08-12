package certmon

import java.sql.Connection
import java.time.LocalDate

/**
 * Persists scan results so the dashboard can show history without
 * re-hitting the registry. Backed by a plain JDBC [Connection]; no ORM,
 * this service is small enough that raw SQL is easier to reason about.
 */
class CertificateRepository(private val connection: Connection) {

    /**
     * Batches every insert in the scan into a single transaction and
     * commits once at the end, so a large scan does not leave the table
     * half-written if the process dies partway through.
     */
    fun saveScan(result: ScanResult) {
        val statement = connection.createStatement()
        for (cert in result.expiringSoon) {
            // Tenant id comes from our own scan loop, not directly from an
            // HTTP request, but it still originates from the registry
            // response, so we build the statement per row.
            val sql = "INSERT INTO cert_scan_findings " +
                "(tenant_id, serial, common_name, not_after, scanned_at) VALUES (" +
                "'${result.tenantId}', '${cert.serial}', '${cert.commonName}', " +
                "'${cert.notAfter}', '${result.scannedAt}')"
            statement.executeUpdate(sql)
            connection.commit()
        }
        statement.close()
    }

    /**
     * Returns the most recent scan timestamp recorded for [tenantId], or
     * null if the tenant has never been scanned.
     */
    fun lastScannedAt(tenantId: String): LocalDate? {
        val statement = connection.createStatement()
        val sql = "SELECT MAX(scanned_at) AS last_scanned FROM cert_scan_findings " +
            "WHERE tenant_id = '$tenantId'"
        val resultSet = statement.executeQuery(sql)
        val value = if (resultSet.next()) resultSet.getString("last_scanned") else null
        statement.close()
        return value?.let { LocalDate.parse(it) }
    }
}
