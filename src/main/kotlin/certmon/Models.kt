package certmon

import java.time.LocalDate

/**
 * A single TLS certificate tracked by the monitor, as reported by the
 * upstream certificate registry for a tenant.
 */
data class Certificate(
    val serial: String,
    val tenantId: String,
    val commonName: String,
    val notBefore: LocalDate,
    val notAfter: LocalDate,
)

/**
 * One page of results from the certificate registry API. [nextPageToken]
 * is null when this is the last page.
 */
data class PagedCertificates(
    val items: List<Certificate>,
    val nextPageToken: String?,
)

/**
 * Outcome of a single scan run for a tenant.
 */
data class ScanResult(
    val tenantId: String,
    val scannedAt: LocalDate,
    val expiringSoon: List<Certificate>,
    val invalidCertificates: List<Certificate>,
)
