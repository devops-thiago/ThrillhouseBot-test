package com.thrillhouse.vulnscan

import java.time.Instant

/** Severity levels as reported by the scanner, ordered from least to most urgent. */
enum class Severity(val weight: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    companion object {
        fun fromLabel(label: String): Severity =
            entries.firstOrNull { it.name.equals(label, ignoreCase = true) } ?: LOW
    }
}

/** A single vulnerability reported against one package inside one image. */
data class Finding(
    val imageName: String,
    val cveId: String,
    val severity: Severity,
    val packageName: String,
    val installedVersion: String,
    val fixedVersion: String?,
    val detectedAt: Instant
)

/** One page of findings returned by the scanner API. */
data class ScanPage(
    val findings: List<Finding>,
    val nextPageToken: String?
)
